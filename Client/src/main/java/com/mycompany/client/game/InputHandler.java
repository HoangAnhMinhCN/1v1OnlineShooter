package com.mycompany.client.game;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;

import java.util.HashSet;
import java.util.Set;

/**
 * Xử lý keyboard input và ánh xạ vào lệnh điều khiển Tank (top-down 4 hướng).
 *
 * W = lên | S = xuống | A = trái | D = phải
 */
public class InputHandler {

    private final Set<KeyCode> pressedKeys = new HashSet<>();
    private final Tank tank;

    public InputHandler(Scene scene, Tank tank) {
        this.tank = tank;
        registerListeners(scene);
    }

    private void registerListeners(Scene scene) {
        scene.setOnKeyPressed(e -> {
            pressedKeys.add(e.getCode());
            updateTankControls();
        });

        scene.setOnKeyReleased(e -> {
            pressedKeys.remove(e.getCode());
            updateTankControls();
        });
    }

    private void updateTankControls() {
        tank.setMoveUp   (pressedKeys.contains(KeyCode.W) || pressedKeys.contains(KeyCode.UP));
        tank.setMoveDown (pressedKeys.contains(KeyCode.S) || pressedKeys.contains(KeyCode.DOWN));
        tank.setMoveLeft (pressedKeys.contains(KeyCode.A) || pressedKeys.contains(KeyCode.LEFT));
        tank.setMoveRight(pressedKeys.contains(KeyCode.D) || pressedKeys.contains(KeyCode.RIGHT));
    }
}
