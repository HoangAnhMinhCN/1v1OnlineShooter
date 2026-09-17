package com.mycompany.client.game;

import java.util.List;

import com.mycompany.client.Client;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/** Vẽ các viên đạn đang hoạt động và chọn màu theo người chơi. */
public final class BulletRenderer {
    private final Image bulletP1;
    private final Image bulletP2;

    public BulletRenderer() {
        bulletP1 = loadImage("/images/bulletGreen_outline.png");
        bulletP2 = loadImage("/images/bulletRed_outline.png");
    }

    public void render(GraphicsContext gc, List<Bullet> bullets) {
        if (bullets == null || bullets.isEmpty()) {
            return;
        }

        for (Bullet bullet : bullets) {
            if (bullet == null || !bullet.isActive()) {
                continue;
            }

            gc.save();
            // Mỗi viên đạn có vị trí và góc riêng nên phải lưu/khôi phục canvas.
            gc.translate(bullet.getX(), bullet.getY());
            gc.rotate(bullet.getAngle());
            gc.drawImage(resolveImage(bullet), -4, -7, 8, 14);
            gc.restore();
        }
    }

    private Image resolveImage(Bullet bullet) {
        Client client = Client.getInstance();
        // Player 1 dùng đạn xanh, Player 2 dùng đạn đỏ.
        // Màu được xác định theo ID người bắn và số player của client hiện tại.
        boolean localBullet = client != null
                && client.getPlayerId() != null
                && client.getPlayerId().equals(bullet.getIdPlayer());
        boolean playerOneUsesGreen = client == null || client.getMyNumber() == 1;
        boolean useGreen = localBullet == playerOneUsesGreen;
        return useGreen ? bulletP1 : bulletP2;
    }

    private Image loadImage(String path) {
        return new Image(getClass().getResourceAsStream(path));
    }
}
