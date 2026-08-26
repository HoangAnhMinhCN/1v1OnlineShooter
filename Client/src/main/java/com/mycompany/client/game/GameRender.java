package com.mycompany.client.game;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Vòng lặp game chạy ở ~60 FPS.
 * Render tile map → xe tăng → HUD overlay.
 */
public class GameRender extends AnimationTimer {

    private final GraphicsContext gc;

    // ── Tile images ───────────────────────────────────────────────────────────
    private Image grassTile;
    private Image wallTile;
    private Image sandTile;

    // ── Đối tượng game ────────────────────────────────────────────────────────
    private Tank localTank; // xe tăng của người chơi hiện tại

    public GameRender(GraphicsContext gc) {
        this.gc = gc;
        try {
            grassTile = new Image(getClass().getResourceAsStream("/images/treeSmall.png"));
            wallTile  = new Image(getClass().getResourceAsStream("/images/sandbagBrown.png"));
            sandTile  = new Image(getClass().getResourceAsStream("/images/sand.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Khởi tạo xe tăng ─────────────────────────────────────────────────────

    /**
     * Đặt xe tăng của người chơi cục bộ.
     */
    public void setLocalTank(Tank tank) {
        this.localTank = tank;
    }

    // ── Game loop ─────────────────────────────────────────────────────────────

    @Override
    public void handle(long now) {
        // 1. Cập nhật logic
        if (localTank != null) localTank.update();

        // 2. Xóa canvas
        gc.clearRect(0, 0, GameMap.COLS * GameMap.TILE_SIZE, GameMap.ROWS * GameMap.TILE_SIZE);

        // 3. Render: nền tile + đường đi
        renderTileBase();

        // 4. Render xe tăng (dưới lớp bụi rậm)
        if (localTank != null) renderTank(localTank);

        // 5. Render bụi rậm đè lên xe (lớp trên cùng)
        renderBushOverlay();

        // 6. Render tường
        renderWalls();
    }

    // ── Render Tile Map (3 pass) ────────────────────────────────────────────

    /** Pass 1 — Vẽ nền cát và đường đi cho tất cả tile */
    private void renderTileBase() {
        for (int row = 0; row < GameMap.ROWS; row++) {
            for (int col = 0; col < GameMap.COLS; col++) {
                double x = col * GameMap.TILE_SIZE;
                double y = row * GameMap.TILE_SIZE;
                gc.drawImage(sandTile, x, y, GameMap.TILE_SIZE, GameMap.TILE_SIZE);
            }
        }
    }

    /**
     * Pass 2 — Vẽ bụi rậm (ảnh cỏ) đè lên xe tăng.
     * Render bụi với opacity nhẹ để tạo cảm giác xe bị che khuất bắi cây.
     */
    private void renderBushOverlay() {
        for (int row = 0; row < GameMap.ROWS; row++) {
            for (int col = 0; col < GameMap.COLS; col++) {
                if (GameMap.MAP_DATA[row][col] == 0) {
                    double x = col * GameMap.TILE_SIZE;
                    double y = row * GameMap.TILE_SIZE;
                    // Vẽ ảnh cỏ mờ nhẹ (0.85) để xe vẫn thấy mờ phía dưới
                    gc.setGlobalAlpha(0.85);
                    gc.drawImage(grassTile, x, y, GameMap.TILE_SIZE, GameMap.TILE_SIZE);
                    gc.setGlobalAlpha(1.0);
                }
            }
        }
    }

    /** Pass 3 — Vẽ tường */
    private void renderWalls() {
        for (int row = 0; row < GameMap.ROWS; row++) {
            for (int col = 0; col < GameMap.COLS; col++) {
                if (GameMap.MAP_DATA[row][col] == 1) {
                    double x = col * GameMap.TILE_SIZE;
                    double y = row * GameMap.TILE_SIZE;
                    gc.drawImage(wallTile, x, y, GameMap.TILE_SIZE, GameMap.TILE_SIZE);
                }
            }
        }
    }

    // ── Render Xe Tăng ────────────────────────────────────────────────────────

    /**
     * Vẽ xe tăng bằng các hình học (không cần ảnh).
     * Áp dụng opacity dựa trên tank.getOpacity() — mờ khi trong bụi rậm.
     */
    private void renderTank(Tank tank) {
        double cx = tank.getCenterX();
        double cy = tank.getCenterY();

        // ── Áp dụng opacity ────────────────────────────────────────────────────
        gc.setGlobalAlpha(tank.getOpacity());

        gc.save();

        // ── Xoay theo góc thân xe ─────────────────────────────────────────────
        gc.translate(cx, cy);
        gc.rotate(tank.getAngle());

        double hw = Tank.WIDTH  / 2.0;
        double hh = Tank.HEIGHT / 2.0;

        // ── Bánh xích trái ────────────────────────────────────────────────────
        Color trackColor = Color.web("#2d2d2d");
        gc.setFill(trackColor);
        gc.fillRoundRect(-hw - 3, -hh, 7, Tank.HEIGHT, 3, 3);

        // ── Bánh xích phải ───────────────────────────────────────────────────
        gc.fillRoundRect(hw - 4, -hh, 7, Tank.HEIGHT, 3, 3);

        // ── Thân xe ──────────────────────────────────────────────────────────
        Color bodyCol = Color.web(tank.getBodyColor());
        gc.setFill(bodyCol);
        gc.fillRoundRect(-hw + 2, -hh + 3, Tank.WIDTH - 4, Tank.HEIGHT - 6, 6, 6);

        // ── Chi tiết thân (sọc ngang nhạt hơn) ───────────────────────────────
        Color bodyLight = bodyCol.brighter().deriveColor(0, 1, 1.15, 1);
        gc.setFill(bodyLight);
        gc.fillRoundRect(-hw + 4, -hh + 6, Tank.WIDTH - 8, 6, 3, 3);

        // ── Tháp pháo ────────────────────────────────────────────────────────
        gc.rotate(tank.getTurretAngle()); // xoay thêm góc tháp pháo

        Color turretCol = Color.web(tank.getTurretColor());
        gc.setFill(turretCol);
        gc.fillOval(-9, -9, 18, 18);

        // ── Nòng pháo ────────────────────────────────────────────────────────
        gc.setFill(turretCol.darker());
        gc.fillRoundRect(-2.5, -hh - 6, 5, hh + 4, 2, 2);

        // ── Đầu nòng (vành sáng) ─────────────────────────────────────────────
        gc.setFill(turretCol.brighter());
        gc.fillRect(-3, -hh - 8, 6, 4);

        gc.restore();

        // ── Reset opacity sau khi vẽ xe ──────────────────────────────────────────────
        gc.setGlobalAlpha(1.0);
    }
}
