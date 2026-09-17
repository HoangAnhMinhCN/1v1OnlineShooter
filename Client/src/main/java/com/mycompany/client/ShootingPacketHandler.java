package com.mycompany.client;

import com.mycompany.client.game.GameScene;

import javafx.application.Platform;

/** Handles SHOOT packets received from the server. */
public final class ShootingPacketHandler {
    private ShootingPacketHandler() {
    }

    public static void handle(String[] parts) {
        // SHOOT|playerId|x|y|angle
        if (parts.length != 5) {
            System.err.println("[ShootingPacketHandler] Invalid SHOOT packet");
            return;
        }

        try {
            String playerId = parts[1];
            double x = Double.parseDouble(parts[2]);
            double y = Double.parseDouble(parts[3]);
            double angle = Double.parseDouble(parts[4]);
            Platform.runLater(() -> GameScene.getInstance().spawnBullet(x, y, angle, playerId));
        } catch (NumberFormatException e) {
            System.err.println("[ShootingPacketHandler] Invalid SHOOT values");
        }
    }
}
