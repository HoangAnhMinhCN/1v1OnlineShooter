package com.mycompany.client.game;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;

import java.util.HashSet;
import java.util.Set;

import com.mycompany.client.controller.GameController;

/**
 * Xử lý keyboard input và ánh xạ vào lệnh điều khiển Tank (top-down 4 hướng).
 *
 * W = lên | S = xuống | A = trái | D = phải
 */
public class InputHandler {

    private final Set<KeyCode> pressedKeys = new HashSet<>();
    private final Tank localTank;
    private final GameController gameController;
    private final Scene scene;

    public InputHandler(Scene scene, Tank localTank, GameController gameController) {
        this.scene = scene;
        this.localTank = localTank;
        this.gameController = gameController;
    }

    public void registerListeners(Canvas gameCanvas) {
        scene.setOnKeyPressed(e -> {
            pressedKeys.add(e.getCode());
            updateTankControls();
        });

        scene.setOnKeyReleased(e -> {
            pressedKeys.remove(e.getCode());
            updateTankControls();
        });

        gameCanvas.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (gameController != null) {
                    gameController.onShootPressed();
                }
            }
        });
    }

    public void turretControls(Canvas gameCanvas) {
        gameCanvas.setOnMouseMoved(event -> {
            // 1. Tọa độ con trỏ chuột trên Canvas
            double mouseX = event.getX();
            double mouseY = event.getY();

            // 2. Tọa độ tâm xe tăng của người chơi
            double tankX = localTank.getCenterX();
            double tankY = localTank.getCenterY();

            // 3. Tính khoảng cách dy, dx
            double dx = mouseX - tankX;
            double dy = mouseY - tankY;

            // 4. Tính góc tính bằng Độ (Degree) từ tâm xe đến chuột
            // Math.atan2 trả về Radian trong khoảng (-PI đến PI), cần đổi sang Degree
            double angleInDegrees = Math.toDegrees(Math.atan2(dy, dx));

            /*
             * LƯU Ý QUAN TRỌNG:
             * - Hàm Math.atan2 trả về 0 độ khi chuột nằm ở phía BÊN PHẢI xe tăng (hướng 3
             * giờ).
             * - Trong hàm renderTank của bạn, nòng pháo được vẽ chĩa lên BÊN TRÊN (-hh - 6,
             * tức hướng 12 giờ).
             * - Do đó, cần +90 độ để bù lệch giữa hướng vẽ mặc định và hướng chuẩn toán
             * học.
             */
            double turretAngle = angleInDegrees + 90;

            // 5. Cập nhật góc tháp pháo cho xe tăng
            localTank.setTurretAngle(turretAngle);

            // (Nếu là game Multiplayer) Gửi góc mới này lên Server
            // sendTurretAngleToServer(turretAngle);
        });
    }

    private void updateTankControls() {
        localTank.setMoveUp(pressedKeys.contains(KeyCode.W) || pressedKeys.contains(KeyCode.UP));
        localTank.setMoveDown(pressedKeys.contains(KeyCode.S) || pressedKeys.contains(KeyCode.DOWN));
        localTank.setMoveLeft(pressedKeys.contains(KeyCode.A) || pressedKeys.contains(KeyCode.LEFT));
        localTank.setMoveRight(pressedKeys.contains(KeyCode.D) || pressedKeys.contains(KeyCode.RIGHT));
    }
}
