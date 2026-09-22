package com.mycompany.server.game;

import com.mycompany.server.GameRoom;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GameEngine {
    private final Map<String, ServerTank> tanksByPlayerId = new ConcurrentHashMap<>();
    private final Map<String, GameRoom> roomsById = new ConcurrentHashMap<>();
    private long serverTick;

    public void createMatch(GameRoom room) {
        roomsById.put(room.getRoomId(), room);

        //tính vị trí xuất hiện hai xe
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
//kiểm tra người chơi có thuộc phòng không
        boolean belongsToRoom = playerId.equals(room.getPlayer1Id())
                || playerId.equals(room.getPlayer2Id());

        if (!belongsToRoom)
            return false;

        tank.applyInput(seq, keys, turretAngle);
        return true;
    }

    public void tick() {
        serverTick++;

        for (ServerTank tank : tanksByPlayerId.values()) {
            tank.simulateTick();
        }
    }

    public long getServerTick() {
        return serverTick;
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
}
