package com.mycompany.client.game;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;

public class GameScene {
    private Scene scene;
    private GameRender gameRender;

    public GameScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/game.fxml"));
            StackPane root = loader.load();

            Canvas canvas = (Canvas) root.lookup("#gameCanvas");
            this.scene = new Scene(root);

            if (canvas != null) {
                this.gameRender = new GameRender(canvas.getGraphicsContext2D());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startLoop() {
        if (gameRender != null) gameRender.start();
    }

    public void stopLoop() {
        if (gameRender != null) gameRender.stop();
    }

    public Scene getScene() {
        return scene;
    }
}
