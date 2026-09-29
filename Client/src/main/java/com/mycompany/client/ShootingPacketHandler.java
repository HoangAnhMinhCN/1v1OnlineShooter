package com.mycompany.client;

import com.mycompany.client.game.GameScene;

import javafx.application.Platform;

/** Handles SHOOT packets received from the server. */
public final class ShootingPacketHandler {
    private ShootingPacketHandler() {
    }

    public static void handle(String[] parts) {
        // SHOOT|bulletId|playerId|x|y|angle
        if (parts.length != 6) {
            System.err.println("[ShootingPacketHandler] Invalid SHOOT packet");
            return;
        }

        try {
            String bulletId = parts[1];
            String playerId = parts[2];
            double x = Double.parseDouble(parts[3]);
            double y = Double.parseDouble(parts[4]);
            double angle = Double.parseDouble(parts[5]);
            Platform.runLater(() -> GameScene.getInstance().spawnBullet(bulletId, x, y, angle, playerId));
        } catch (NumberFormatException e) {
            System.err.println("[ShootingPacketHandler] Invalid SHOOT values");
        }
    }
}
