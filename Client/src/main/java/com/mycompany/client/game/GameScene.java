package com.mycompany.client.game;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;

/**
 * Khởi tạo scene game: load FXML, tạo xe tăng, kết nối GameRender + InputHandler.
 */
public class GameScene {

    private Scene scene;
    private GameRender gameRender;
    private Tank localTank;
    private InputHandler inputHandler;

    public GameScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/game.fxml"));
            StackPane root = loader.load();

            Canvas canvas = (Canvas) root.lookup("#gameCanvas");
            this.scene = new Scene(root);

            if (canvas != null) {
                this.gameRender = new GameRender(canvas.getGraphicsContext2D());

                // ── Khởi tạo xe tăng Player 1 ──────────────────────────────
                // Đặt ở góc trên-trái (tile [1][1] = ô cỏ)
                double startX = 1 * GameMap.TILE_SIZE + (GameMap.TILE_SIZE - Tank.WIDTH)  / 2.0;
                double startY = 1 * GameMap.TILE_SIZE + (GameMap.TILE_SIZE - Tank.HEIGHT) / 2.0;

                localTank = new Tank(startX, startY, "#4a7c59", "#2e5436");
                gameRender.setLocalTank(localTank);

                // ── Kết nối InputHandler ───────────────────────────────────
                inputHandler = new InputHandler(scene, localTank);
                inputHandler.registerListeners(scene);
                inputHandler.turretControls(canvas);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Vòng lặp game ─────────────────────────────────────────────────────────

    public void startLoop() {
        if (gameRender != null) gameRender.start();
    }

    public void stopLoop() {
        if (gameRender != null) gameRender.stop();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Scene getScene() {
        return scene;
    }

    public Tank getLocalTank() {
        return localTank;
    }
}
