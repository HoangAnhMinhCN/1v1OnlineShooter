package com.mycompany.client;

import com.mycompany.client.game.GameScene;
import com.mycompany.client.game.Tank;

import javafx.application.Platform;

/** Handles MOVE packets received from the server. */
public final class MovementPacketHandler {
    private MovementPacketHandler() {
    }

    public static void handle(String[] parts) {
        // MOVE|playerId|x|y|bodyAngle|turretAngle
        if (parts.length != 6) {
            System.err.println("[MovementPacketHandler] Invalid MOVE packet");
            return;
        }

        try {
            String playerId = parts[1];
            double x = Double.parseDouble(parts[2]);
            double y = Double.parseDouble(parts[3]);
            double bodyAngle = Double.parseDouble(parts[4]);
            double turretAngle = Double.parseDouble(parts[5]);

            Platform.runLater(() -> {
                Tank enemy = GameScene.getInstance().getTank(playerId);
                if (enemy != null) {
                    enemy.setTargetPosition(x, y);
                    enemy.setTurretAngle(turretAngle);
                    enemy.setAngle(bodyAngle);
                }
            });
        } catch (NumberFormatException e) {
            System.err.println("[MovementPacketHandler] Invalid MOVE values");
        }
    }
}
