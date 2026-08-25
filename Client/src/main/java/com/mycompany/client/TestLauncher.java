package com.mycompany.client;

import com.mycompany.client.game.GameScene;

import javafx.application.Application;
import javafx.stage.Stage;

public class TestLauncher extends Application {
    private GameScene gameScene;

    @Override
    public void start(Stage stage) throws Exception {
        gameScene = new GameScene();
        
        stage.setTitle("1v1 Online Shooter");
        stage.setScene(gameScene.getScene());
        stage.setResizable(false);
        stage.show();
        
        gameScene.startLoop();
    }

    public static void main(String[] args) {
        launch();
    }
}
