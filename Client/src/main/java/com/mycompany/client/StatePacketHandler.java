package com.mycompany.client;

import java.util.ArrayList;
import java.util.List;

import com.mycompany.client.game.GameScene;
import com.mycompany.client.game.Tank;
import javafx.application.Platform;

/** Applies authoritative UDP STATE snapshots sent by the server. */
public final class StatePacketHandler {
    private static long lastReceivedServerTick = -1;
    private static long lastAcknowledgedInputSeq = -1;

    private StatePacketHandler() {
    }

    public static void handle(String[] parts) {
        // ack server đã chấp nhận iput có thứ tự ack
        // STATE|roomId|tick|ack1|ack2|x1|y1|body1|turret1|hp1|x2|y2|body2|turret2|hp2
        if (parts.length < 15) {
            System.err.println("[StatePacketHandler] Invalid STATE packet");
            return;
        }

        try {
            Client client = Client.getInstance();
            if (client == null || !parts[1].equals(client.getGameRoomId())) {
                return;
            }

            long serverTick = Long.parseLong(parts[2]);
            long ack1 = Long.parseLong(parts[3]);
            long ack2 = Long.parseLong(parts[4]);

            double x1 = Double.parseDouble(parts[5]);
            double y1 = Double.parseDouble(parts[6]);
            double body1 = Double.parseDouble(parts[7]);
            double turret1 = Double.parseDouble(parts[8]);
            int hp1 = Integer.parseInt(parts[9]);
            double x2 = Double.parseDouble(parts[10]);
            double y2 = Double.parseDouble(parts[11]);
            double body2 = Double.parseDouble(parts[12]);
            double turret2 = Double.parseDouble(parts[13]);
            int hp2 = Integer.parseInt(parts[14]);

            if (serverTick < 0 || !Double.isFinite(x1) || !Double.isFinite(y1)
                    || !Double.isFinite(body1) || !Double.isFinite(turret1)
                    || !Double.isFinite(x2) || !Double.isFinite(y2)
                    || !Double.isFinite(body2) || !Double.isFinite(turret2)) {
                return;
            }

            // Parse danh sách ID đạn ĐÃ BỊ HỦY (không còn active)
            List<String> destroyedBulletIds = new ArrayList<>();
            if (parts.length > 15) {
                int destroyedCount = Integer.parseInt(parts[15]);
                if (parts.length >= 16 + destroyedCount) {
                    for (int i = 0; i < destroyedCount; i++) {
                        destroyedBulletIds.add(parts[16 + i]);
                    }
                }
            }

            synchronized (StatePacketHandler.class) {
                if (serverTick <= lastReceivedServerTick) {
                    return;
                }
                lastReceivedServerTick = serverTick;
                lastAcknowledgedInputSeq = client.getMyNumber() == 1 ? ack1 : ack2;
            }

            boolean amPlayer1 = client.getMyNumber() == 1;
            // cập nhật giao diện
            Platform.runLater(() -> applyState(
                    client, amPlayer1,
                    x1, y1, body1, turret1, hp1,
                    x2, y2, body2, turret2, hp2, destroyedBulletIds));
        } catch (NumberFormatException ignored) {
            System.err.println("[StatePacketHandler] Invalid STATE values");
        }
    }

    public static void handleDestroyedBullet(String[] parts) {
        if (parts.length != 3) {
            return;
        }

        Client client = Client.getInstance();
        if (client == null || !parts[1].equals(client.getGameRoomId())) {
            return;
        }

        String bulletId = parts[2];
        Platform.runLater(() -> {
            GameScene scene = GameScene.getInstance();
            if (scene != null) {
                scene.removeDestroyedBullets(List.of(bulletId));
            }
        });
    }

    private static void applyState(
            Client client, boolean amPlayer1,
            double x1, double y1, double body1, double turret1, int hp1,
            double x2, double y2, double body2, double turret2, int hp2,
            List<String> destroyedBulletIds) {

        GameScene scene = GameScene.getInstance();
        if (scene == null) {
            return;
        }

        // Xóa đạn ngay khi nhận được STATE, không phụ thuộc vào việc tank đã sẵn sàng.
        if (!destroyedBulletIds.isEmpty()) {
            scene.removeDestroyedBullets(destroyedBulletIds);
        }

        Tank localTank = scene.getLocalTank();
        Tank enemyTank = scene.getTank(client.getAnotherPlayerId());
        if (localTank == null || enemyTank == null) {
            return;
        }

        if (amPlayer1) {
            applyLocalState(localTank, x1, y1, body1, turret1, hp1);
            applyRemoteState(enemyTank, x2, y2, body2, turret2, hp2);
        } else {
            applyLocalState(localTank, x2, y2, body2, turret2, hp2);
            applyRemoteState(enemyTank, x1, y1, body1, turret1, hp1);
        }

    }

    private static void applyLocalState(Tank tank, double x, double y, double bodyAngle, double turretAngle, int hp) {
        // Bước reconciliation sau sẽ thay setPosition
        // bằng việc replay INPUT chưa ACK.
        tank.setPosition(x, y);
        tank.setAngle(bodyAngle);
        tank.setTurretAngle(turretAngle);
        tank.setHp(hp);
    }

    private static void applyRemoteState(Tank tank, double x, double y, double bodyAngle, double turretAngle, int hp) {
        tank.setTargetPosition(x, y);
        tank.setAngle(bodyAngle);
        tank.setTurretAngle(turretAngle);
        tank.setHp(hp);
    }

    public static synchronized long getLastAcknowledgedInputSeq() {
        return lastAcknowledgedInputSeq;
    }
}
