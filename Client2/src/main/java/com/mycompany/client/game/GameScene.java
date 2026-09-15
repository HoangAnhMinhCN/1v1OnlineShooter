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

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static GameScene instance;

    // Trả về GameScene đang chạy để ServerHandler lấy tank đối thủ
    public static GameScene getInstance() {
        return instance;
    }

    // ── Fields ────────────────────────────────────────────────────────────────
    private Scene scene;
    private GameRender gameRender;
    private Tank localTank;
    private InputHandler inputHandler;
    private GameController gameController;
    private final List<Bullet> bullets = new CopyOnWriteArrayList<>();

    private List<Tank> tanks;

    // ── Constructor ───────────────────────────────────────────────────────────

    public GameScene(StackPane root, GameController gameController) {
        instance = this; // đăng ký singleton ngay khi tạo GameScene
        this.scene = new Scene(root);
        this.gameController = gameController;

        Canvas canvas = (Canvas) root.lookup("#gameCanvas");

        if (canvas != null) {
            this.gameRender = new GameRender(canvas.getGraphicsContext2D());

            // Tạo danh sách tank và lưu vào field (không dùng biến local)
            // Hai ID cố định dùng cho lần test local: Client = 0, Client 2 = 1.
            tanks = createTanks(0, 1);
            gameRender.setTanks(tanks);

            // Client 2 điều khiển tank có playerId = 1.
            localTank = tanks.get(1);
            // Tank còn lại nhận vị trí từ server và được nội suy khi render.
            tanks.get(0).setRemoteControlled(true);
            gameRender.setLocalTank(localTank);

            gameRender.setBullets(this.bullets);

            // ── Kết nối InputHandler ───────────────────────────────────
            inputHandler = new InputHandler(scene, localTank, this.gameController);
            inputHandler.registerListeners(canvas);
            inputHandler.turretControls(canvas);
        }
    }

    // ── Tạo xe tăng ──────────────────────────────────────────────────────────

    private List<Tank> createTanks(int idPlayer1, int idPlayer2) {
        List<Tank> result = new ArrayList<>();

        // ── Khởi tạo xe tăng Player 1 ──────────────────────────────
        // Đặt ở góc trên-trái (tile [1][1] = ô cỏ)
        double startX = 1 * GameMap.TILE_SIZE + (GameMap.TILE_SIZE - Tank.WIDTH) / 2.0;
        double startY = 1 * GameMap.TILE_SIZE + (GameMap.TILE_SIZE - Tank.HEIGHT) / 2.0;
        Tank tank1 = new Tank(startX, startY, "#4a7c59", "#2e5436", idPlayer1);

        // ── Khởi tạo xe tăng Player 2 ──────────────────────────────
        // Đặt ở góc dưới-phải (tile [n-2][m-2] = ô cỏ)
        startX = (GameMap.COLS - 2) * GameMap.TILE_SIZE + (GameMap.TILE_SIZE - Tank.WIDTH) / 2.0;
        startY = (GameMap.ROWS - 2) * GameMap.TILE_SIZE + (GameMap.TILE_SIZE - Tank.HEIGHT) / 2.0;
        Tank tank2 = new Tank(startX, startY, "#F44336", "#2D2D2D", idPlayer2);

        result.add(tank1);
        result.add(tank2);
        return result;
    }

    // ── Bullet ────────────────────────────────────────────────────────────────

    public void spawnBullet(Tank shooter) {
        if (shooter == null)
            return;

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

    /**
     * Lấy tank đối thủ (tank có idPlayer KHÁC với myPlayerId).
     * ServerHandler gọi để cập nhật vị trí tank địch khi nhận gói MOVE từ server.
     */
    public Tank getEnemyTank(int myPlayerId) {
        return tanks.stream()
                .filter(t -> t.getIdPlayer() != myPlayerId) // lọc ra tank không phải của mình
                .findFirst() // lấy phần tử đầu tiên tìm được
                .orElse(null); // trả null nếu không tìm thấy
    }

    public Tank getTank(int playerId) {
        // Tìm tank tương ứng với playerId server gửi về.
        if (tanks == null) return null;
        return tanks.stream()
                .filter(t -> t.getIdPlayer() == playerId)
                .findFirst()
                .orElse(null);
    }
}
