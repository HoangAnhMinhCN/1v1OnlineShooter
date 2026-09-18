package com.mycompany.client;

import com.mycompany.client.game.Tank;

/** Tạo và gửi các packet gameplay qua UDP. */
public final class GamePacketSender {
    private GamePacketSender() {
    }

    public static void sendMove(Client client, Tank tank) {
        if (client == null || tank == null) {
            return;
        }

        String packet = String.format(
                "MOVE|%s|%s|%.2f|%.2f|%.2f|%.2f",
                client.getGameRoomId(),
                tank.getIdPlayer(),
                tank.getX(),
                tank.getY(),
                tank.getAngle(),
                tank.getTurretAngle());

        client.sendUdpData(packet);
    }

    public static void sendShoot(Client client, Tank tank) {
        if (client == null || tank == null) {
            return;
        }

        String packet = String.format(
                "SHOOT|%s|%s|%.2f|%.2f|%.2f",
                client.getGameRoomId(),
                tank.getIdPlayer(),
                tank.getCenterX(),
                tank.getCenterY(),
                tank.getTurretAngle());

        client.sendUdpData(packet);
    }
}
