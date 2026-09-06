package com.mycompany.client.game;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.mycompany.client.controller.GameController;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;

/**
 * Khởi tạo scene game: load FXML, tạo xe tăng, kết nối GameRender +
 * InputHandler.
 */
public class GameScene {

    private Scene scene;
    private GameRender gameRender;
    private Tank localTank;
    private InputHandler inputHandler;
    private GameController gameController;
    private final List<Bullet> bullets = new CopyOnWriteArrayList<>();

    public GameScene(StackPane root, GameController gameController) {
        this.scene = new Scene(root);
        this.gameController = gameController;

        Canvas canvas = (Canvas) root.lookup("#gameCanvas");

        if (canvas != null) {
            this.gameRender = new GameRender(canvas.getGraphicsContext2D());

            List<Tank> tanks = createTanks(1, 2);  // viết thêm truyền id player
            gameRender.setTanks(tanks);
            localTank = tanks.get(0);
            gameRender.setLocalTank(localTank);

            gameRender.setBullets(this.bullets);

            // ── Kết nối InputHandler ───────────────────────────────────
            inputHandler = new InputHandler(scene, localTank, this.gameController);
            inputHandler.registerListeners(canvas);
            inputHandler.turretControls(canvas);
        } else {

        }
    }

    private List<Tank> createTanks(int idPlayer1, int idPlayer2) {
        List<Tank> tanks = new ArrayList<>();
        // ── Khởi tạo xe tăng Player 1 ──────────────────────────────
        // Đặt ở góc trên-trái (tile [1][1] = ô cỏ)
        double startX = 1 * GameMap.TILE_SIZE + (GameMap.TILE_SIZE - Tank.WIDTH) / 2.0;
        double startY = 1 * GameMap.TILE_SIZE + (GameMap.TILE_SIZE - Tank.HEIGHT) / 2.0;

        Tank tank1 = new Tank(startX, startY, "#4a7c59", "#2e5436", 1);

        // ── Khởi tạo xe tăng Player 2 ──────────────────────────────
        // Đặt ở góc dưới-phải (tile [n-2][m-2] = ô cỏ)
        startX = (GameMap.COLS - 2) * GameMap.TILE_SIZE + (GameMap.TILE_SIZE - Tank.WIDTH) / 2.0;
        startY = (GameMap.ROWS - 2) * GameMap.TILE_SIZE + (GameMap.TILE_SIZE - Tank.HEIGHT) / 2.0;

        Tank tank2 = new Tank(startX, startY, "#F44336", "#2D2D2D", 2);

        tanks.add(tank1);
        tanks.add(tank2);

        return tanks;
    }

    public void spawnBullet(Tank shooter) {
        if (shooter == null) return;

        // Tọa độ nòng pháo / tâm xe
        double startX = shooter.getCenterX();
        double startY = shooter.getCenterY();
        double angle = shooter.getTurretAngle();

        // Tạo đối tượng đạn mới và thêm vào list
        Bullet newBullet = new Bullet(startX, startY, angle, shooter.getIdPlayer());
        bullets.add(newBullet);
    }

    // ── Vòng lặp game ─────────────────────────────────────────────────────────

    public void startLoop() {
        if (gameRender != null)
            gameRender.start();
    }

    public void stopLoop() {
        if (gameRender != null)
            gameRender.stop();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Scene getScene() {
        return scene;
    }

    public Tank getLocalTank() {
        return localTank;
    }
}
