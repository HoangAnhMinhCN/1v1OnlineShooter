package com.mycompany.server.game;

import com.mycompany.server.GameRoom;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public final class GameEngine {
    public record DestroyedBullet(String roomId, String bulletId) { }

    private final Map<String, ServerTank> tanksByPlayerId = new ConcurrentHashMap<>();
    private final Map<String, GameRoom> roomsById = new ConcurrentHashMap<>();
    private Map<String, List<ServerBullet>> bulletsByRoomId = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<DestroyedBullet> pendingDestroyedBullets = new ConcurrentLinkedQueue<>();
    private long serverTick;

    public void createMatch(GameRoom room) {
        roomsById.put(room.getRoomId(), room);

        // tính vị trí xuất hiện hai xe
        double spawn1X = 1 * GameMap.TILE_SIZE + 2;
        double spawn1Y = 1 * GameMap.TILE_SIZE + 2;
        double spawn2X = (GameMap.COLS - 2) * GameMap.TILE_SIZE + 2;
        double spawn2Y = (GameMap.ROWS - 2) * GameMap.TILE_SIZE + 2;

        tanksByPlayerId.put(room.getPlayer1Id(),
                new ServerTank(room.getPlayer1Id(), spawn1X, spawn1Y));
        tanksByPlayerId.put(room.getPlayer2Id(),
                new ServerTank(room.getPlayer2Id(), spawn2X, spawn2Y));
    }

    public boolean applyInput(
            String roomId, String playerId, long seq, int keys, double turretAngle) {

        GameRoom room = roomsById.get(roomId);
        ServerTank tank = tanksByPlayerId.get(playerId);

        if (room == null || tank == null)
            return false;
        // kiểm tra người chơi có thuộc phòng không
        boolean belongsToRoom = playerId.equals(room.getPlayer1Id())
                || playerId.equals(room.getPlayer2Id());

        if (!belongsToRoom)
            return false;

        tank.applyInput(seq, keys, turretAngle);
        return true;
    }

    public void tick() {
        serverTick++;

        // 1. Cập nhật di chuyển Tank
        for (ServerTank tank : tanksByPlayerId.values()) {
            tank.simulateTick();
        }

        // 2. Cập nhật và xử lý va chạm Đạn
        for (Map.Entry<String, List<ServerBullet>> entry : bulletsByRoomId.entrySet()) {
            String roomId = entry.getKey();
            List<ServerBullet> bullets = entry.getValue();
            GameRoom room = roomsById.get(roomId);
            if (room == null)
                continue;

            ServerTank p1 = tanksByPlayerId.get(room.getPlayer1Id());
            ServerTank p2 = tanksByPlayerId.get(room.getPlayer2Id());

            for (ServerBullet bullet : bullets) {
                if (!bullet.isAlive())
                    continue;

                // Cập nhật vị trí đạn bay
                bullet.updatePosition();

                // --- A. VA CHẠM ĐẠN - TƯỜNG ---
                if (isBulletCollidingWithMap(bullet)) {
                    bullet.destroy();
                    pendingDestroyedBullets.add(new DestroyedBullet(roomId, bullet.getId()));
                    continue;
                }

                // --- B. VA CHẠM ĐẠN - XE TANK ---
                // Kiểm tra va chạm với Player 1
                if (p1 != null && !p1.isDead() && !bullet.getOwnerId().equals(p1.getPlayerId())) {
                    if (p1.intersectsBullet(bullet.getX(), bullet.getY(), bullet.getRadius())) {
                        p1.takeDamage(ServerBullet.BULLET_DAMAGE);
                        bullet.destroy();
                        pendingDestroyedBullets.add(new DestroyedBullet(roomId, bullet.getId()));
                        checkGameOver(room, p1, p2);
                        continue;
                    }
                }

                // // Kiểm tra va chạm với Player 2
                if (p2 != null && !p2.isDead() && !bullet.getOwnerId().equals(p2.getPlayerId())) {
                    if (p2.intersectsBullet(bullet.getX(), bullet.getY(), bullet.getRadius())) {
                        p2.takeDamage(ServerBullet.BULLET_DAMAGE);
                        bullet.destroy();
                        pendingDestroyedBullets.add(new DestroyedBullet(roomId, bullet.getId()));
                        checkGameOver(room, p1, p2);
                        continue;
                    }
                }
            }

            // Xóa các viên đạn đã nổ / biến mất khỏi memory
            //bullets.removeIf(b -> !b.isAlive());
        }
    }

    private boolean isBulletCollidingWithMap(ServerBullet bullet) {
        double radius = bullet.getRadius();
        double mapWidth = GameMap.COLS * GameMap.TILE_SIZE;
        double mapHeight = GameMap.ROWS * GameMap.TILE_SIZE;
        if (bullet.getX() - radius < 0 || bullet.getY() - radius < 0
                || bullet.getX() + radius >= mapWidth
                || bullet.getY() + radius >= mapHeight) {
            return true;
        }

        int minCol = (int) ((bullet.getX() - radius) / GameMap.TILE_SIZE);
        int maxCol = (int) ((bullet.getX() + radius) / GameMap.TILE_SIZE);
        int minRow = (int) ((bullet.getY() - radius) / GameMap.TILE_SIZE);
        int maxRow = (int) ((bullet.getY() + radius) / GameMap.TILE_SIZE);

        for (int row = minRow; row <= maxRow; row++) {
            for (int col = minCol; col <= maxCol; col++) {
                if (GameMap.MAP_DATA[row][col] == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    public void addBullet(String roomId, String playerId, String bulletId, double x, double y, double angleRad) {
        // computeIfAbsent giúp khởi tạo list an toàn atomically nếu roomId chưa tồn tại
        List<ServerBullet> bullets = bulletsByRoomId.computeIfAbsent(
                roomId,
                k -> new CopyOnWriteArrayList<>());

        bullets.add(new ServerBullet(bulletId, roomId, playerId, x, y, angleRad));
    }

    /** Xử lý yêu cầu bắn đạn từ Client */
    public void spawnBullet(String roomId, String bulletId, String playerId, double x, double y, double angleRad) {
        GameRoom room = roomsById.get(roomId);
        ServerTank tank = tanksByPlayerId.get(playerId);

        if (room == null || tank == null || tank.isDead()) {
            return;
        }

        addBullet(roomId, playerId, bulletId, x, y, angleRad);
    }

    /** Kiểm tra va chạm giữa Đạn và Tường */
    // private boolean isBulletCollidingWithMap(ServerBullet bullet) {
    //     int col = (int) (bullet.getX() / GameMap.TILE_SIZE);
    //     int row = (int) (bullet.getY() / GameMap.TILE_SIZE);

    //     return GameMap.isSolid(row, col);
    // }

    private void checkGameOver(GameRoom room, ServerTank p1, ServerTank p2) {
        if (p1.isDead() || p2.isDead()) {
            String winnerId = p1.isDead() ? p2.getPlayerId() : p1.getPlayerId();
            System.out.println("[GameEngine] Trận đấu " + room.getRoomId() + " kết thúc! Người thắng: " + winnerId);
            // TODO: Gửi sự kiện GAME_OVER qua TCP cho 2 client
        }
    }

    public long getServerTick() {
        return serverTick;
    }

    public List<DestroyedBullet> drainDestroyedBullets() {
        List<DestroyedBullet> destroyedBullets = new ArrayList<>();
        DestroyedBullet destroyedBullet;
        while ((destroyedBullet = pendingDestroyedBullets.poll()) != null) {
            destroyedBullets.add(destroyedBullet);
        }
        return destroyedBullets;
    }

    public ServerTank getTank(String playerId) {
        return tanksByPlayerId.get(playerId);
    }

    public GameRoom getRoom(String roomId) {
        return roomsById.get(roomId);
    }

    /** Returns the active rooms managed by this engine. */
    public Collection<GameRoom> getRooms() {
        return roomsById.values();
    }

    public List<ServerBullet> getBullets(String roomId) {
        return bulletsByRoomId.getOrDefault(roomId, List.of());
    }
}
