package com.mycompany.client.game;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class GameRender extends AnimationTimer {
    private final GraphicsContext gc;

    private Image grassTile;
    private Image wallTile;
    private Image sandTile;

    public GameRender(GraphicsContext gc) {
        this.gc = gc;
        try {
            grassTile = new Image(getClass().getResourceAsStream("/images/treeSmall.png"));
            wallTile = new Image(getClass().getResourceAsStream("/images/sandbagBrown.png"));
            sandTile = new Image(getClass().getResourceAsStream("/images/sand.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handle(long now) {
        gc.clearRect(0, 0, GameMap.COLS * GameMap.TILE_SIZE, GameMap.ROWS * GameMap.TILE_SIZE);
        renderTileMap();
    }

    private void renderTileMap() {
        for (int row = 0; row < GameMap.ROWS; row++) {
            for (int col = 0; col < GameMap.COLS; col++) {
                int tileType = GameMap.MAP_DATA[row][col];
                double x = col * GameMap.TILE_SIZE;
                double y = row * GameMap.TILE_SIZE;

                gc.drawImage(sandTile, x, y, GameMap.TILE_SIZE, GameMap.TILE_SIZE);

                if (tileType == 0) {
                    gc.drawImage(grassTile, x, y, GameMap.TILE_SIZE, GameMap.TILE_SIZE);
                } 
                else if (tileType == 1) {
                    gc.drawImage(wallTile, x, y, GameMap.TILE_SIZE, GameMap.TILE_SIZE);
                }
            }
        }
    }
}
