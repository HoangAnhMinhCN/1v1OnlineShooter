package com.mycompany.client.game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/** Quản lý ảnh và vẽ các layer của bản đồ game. */
public final class GameMapRenderer {
    private final Image grassTile;
    private final Image wallTile;
    private final Image sandTile;

    public GameMapRenderer() {
        grassTile = loadImage("/images/treeSmall.png");
        wallTile = loadImage("/images/sandbagBrown.png");
        sandTile = loadImage("/images/sand.png");
    }

    public void renderTileBase(GraphicsContext gc) {
        // Vẽ nền cát cho toàn bộ các ô bản đồ.
        for (int row = 0; row < GameMap.ROWS; row++) {
            for (int col = 0; col < GameMap.COLS; col++) {
                drawTile(gc, sandTile, row, col);
            }
        }
    }

    public void renderBushOverlay(GraphicsContext gc) {
        // Bụi cây được vẽ sau tank để tạo hiệu ứng tank bị che khuất.
        gc.save();
        gc.setGlobalAlpha(0.85);
        for (int row = 0; row < GameMap.ROWS; row++) {
            for (int col = 0; col < GameMap.COLS; col++) {
                if (GameMap.MAP_DATA[row][col] == 0) {
                    drawTile(gc, grassTile, row, col);
                }
            }
        }
        gc.restore();
    }

    public void renderWalls(GraphicsContext gc) {
        // Tường được vẽ sau bụi cây để luôn hiển thị như vật cản phía trước.
        for (int row = 0; row < GameMap.ROWS; row++) {
            for (int col = 0; col < GameMap.COLS; col++) {
                if (GameMap.MAP_DATA[row][col] == 1) {
                    drawTile(gc, wallTile, row, col);
                }
            }
        }
    }

    private void drawTile(GraphicsContext gc, Image image, int row, int col) {
        double x = col * GameMap.TILE_SIZE;
        double y = row * GameMap.TILE_SIZE;
        gc.drawImage(image, x, y, GameMap.TILE_SIZE, GameMap.TILE_SIZE);
    }

    private Image loadImage(String path) {
        // Tải tài nguyên một lần khi khởi tạo renderer, không tải lại mỗi frame.
        return new Image(getClass().getResourceAsStream(path));
    }
}
