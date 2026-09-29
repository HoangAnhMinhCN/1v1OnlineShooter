package com.mycompany.client.game;

import com.mycompany.client.Client;
import com.mycompany.client.GamePacketSender;
import java.util.List;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;

/**
 * Vòng lặp chính của game.
 *
 * <p>
 * Lớp này chỉ điều phối việc cập nhật trạng thái, gửi vị trí lên server
 * và yêu cầu renderer vẽ một frame. Logic vẽ chi tiết được tách sang các
 * renderer chuyên trách.
 * </p>
 */
public class GameRender extends AnimationTimer {
    /**
     * Gửi INPUT lên server với tần suất ~60 gói/giây (khớp server tick rate 60Hz).
     *
     * <p>Tăng từ 20/s lên 60/s vì:</p>
     * <ul>
     *   <li>Server tick ở 60Hz → mỗi STATE có thể ACK ~3 input cũ nếu gửi 20/s.</li>
     *   <li>Với 60/s, pendingInputs buffer luôn nhỏ (~2–5 input) → reconciliation nhẹ hơn.</li>
     *   <li>Độ trễ phản hồi từ server giảm vì server nhận input ngay trong tick đó.</li>
     * </ul>
     */
    private static final long POSITION_SEND_INTERVAL_NANOS = 16_666_667L; // ~60 gói/s


    private final GraphicsContext gc;
    private final GameMapRenderer mapRenderer;
    private final BulletRenderer bulletRenderer;
    private List<Bullet> bullets;
    private List<Tank> tanks;
    private Tank localTank;
    private long lastPositionSendNanos;

    public GameRender(GraphicsContext gc) {
        this.gc = gc;
        this.mapRenderer = new GameMapRenderer();
        this.bulletRenderer = new BulletRenderer();
    }

    public void setLocalTank(Tank tank) {
        this.localTank = tank;
    }

    public void setTanks(List<Tank> tanks) {
        this.tanks = tanks;
    }

    public void setBullets(List<Bullet> bullets) {
        this.bullets = bullets;
    }

    @Override
    public void handle(long now) {
        // Cập nhật trạng thái trước, sau đó mới vẽ frame hiện tại.
        updateGameState(now);
        renderFrame();
    }

    private void renderFrame() {
        // Thứ tự layer: nền → xe → bụi cây → tường → đạn.
        gc.clearRect(0, 0,
                GameMap.COLS * GameMap.TILE_SIZE,
                GameMap.ROWS * GameMap.TILE_SIZE);
        mapRenderer.renderTileBase(gc);
        renderTanks();
        mapRenderer.renderBushOverlay(gc);
        mapRenderer.renderWalls(gc);
        bulletRenderer.render(gc, bullets);
    }

    private void renderTanks() {
        if (tanks == null) {
            return;
        }

        for (Tank tank : tanks) {
            // TankRenderer tự quản lý phép xoay, màu sắc và opacity của từng xe.
            TankRenderer.render(gc, tank);
        }
    }

    private void updateGameState(long now) {
        // Đạn và xe tăng được cập nhật trên JavaFX AnimationTimer (~60 FPS).
        updateBullets();
        updateTanks();

        // Giới hạn tần suất gửi trạng thái di chuyển còn 20 gói/giây.
        if (localTank != null && now - lastPositionSendNanos >= POSITION_SEND_INTERVAL_NANOS) {
            GamePacketSender.sendInput(Client.getInstance(), localTank);
            lastPositionSendNanos = now;
        }
    }

    private void updateTanks() {
        if (localTank != null) {
            localTank.update();
        }

        if (tanks == null) {
            return;
        }

        for (Tank tank : tanks) {
            // Tank cục bộ đã được cập nhật ở trên; các tank còn lại là đối thủ.
            if (tank != null && tank != localTank) {
                tank.update();
            }
        }
    }

    private void updateBullets() {
        if (bullets == null || bullets.isEmpty()) {
            return;
        }

        // 1. Cập nhật vị trí cho tất cả viên đạn
        for (Bullet bullet : bullets) {
            if (bullet != null && bullet.isActive()) {
                bullet.update();
                if (!bullet.isActive() || tanks == null) {
                    continue;
                }

                // Predict only the visual impact; server remains authoritative for damage.
                for (Tank tank : tanks) {
                    if (tank != null && !tank.getIdPlayer().equals(bullet.getIdPlayer())
                            && tank.intersectsBullet(bullet.getX(), bullet.getY(), 4.0)) {
                        bullet.setActive(false);
                        break;
                    }
                }
            }
        }

        // 2. Lọc xóa tất cả đạn inactive/null một cách an toàn
        bullets.removeIf(bullet -> bullet == null || !bullet.isActive());
    }

}
