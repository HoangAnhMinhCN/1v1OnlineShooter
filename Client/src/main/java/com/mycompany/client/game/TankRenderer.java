package com.mycompany.client.game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/** Chỉ chịu trách nhiệm vẽ xe tăng và tháp pháo xoay độc lập. */
public final class TankRenderer {
    private static final Color TRACK_COLOR = Color.web("#2d2d2d");

    private TankRenderer() { }

    public static void render(GraphicsContext gc, Tank tank) {
        if (tank == null) return;

        // Lưu trạng thái canvas để phép xoay/opacity của tank không ảnh hưởng
        // đến các đối tượng được vẽ sau đó.
        gc.save();
        gc.setGlobalAlpha(tank.getOpacity());
        gc.translate(tank.getCenterX(), tank.getCenterY());
        renderBody(gc, tank);
        renderTurret(gc, tank);
        gc.restore();
    }

    private static void renderBody(GraphicsContext gc, Tank tank) {
        // Thân xe và bánh xích xoay theo hướng di chuyển của thân xe.
        gc.save();
        gc.rotate(tank.getAngle());
        double halfWidth = Tank.WIDTH / 2.0;
        double halfHeight = Tank.HEIGHT / 2.0;
        Color bodyColor = Color.web(tank.getBodyColor());
        gc.setFill(TRACK_COLOR);
        gc.fillRoundRect(-halfWidth - 3, -halfHeight, 7, Tank.HEIGHT, 3, 3);
        gc.fillRoundRect(halfWidth - 4, -halfHeight, 7, Tank.HEIGHT, 3, 3);
        gc.setFill(bodyColor);
        gc.fillRoundRect(-halfWidth + 2, -halfHeight + 3, Tank.WIDTH - 4, Tank.HEIGHT - 6, 6, 6);
        gc.setFill(bodyColor.brighter().deriveColor(0, 1, 1.15, 1));
        gc.fillRoundRect(-halfWidth + 4, -halfHeight + 6, Tank.WIDTH - 8, 6, 3, 3);
        gc.restore();
    }

    private static void renderTurret(GraphicsContext gc, Tank tank) {
        // Tháp pháo xoay theo hướng chuột, độc lập với hướng thân xe.
        gc.save();
        gc.rotate(tank.getTurretAngle());
        Color turretColor = Color.web(tank.getTurretColor());
        double halfHeight = Tank.HEIGHT / 2.0;
        gc.setFill(turretColor);
        gc.fillOval(-9, -9, 18, 18);
        gc.setFill(turretColor.darker());
        gc.fillRoundRect(-2.5, -halfHeight - 6, 5, halfHeight + 4, 2, 2);
        gc.setFill(turretColor.brighter());
        gc.fillRect(-3, -halfHeight - 8, 6, 4);
        gc.restore();
    }
}
