package com.mycompany.client.controller;

import java.net.URL;

import com.mycompany.client.Client;
import com.mycompany.client.game.GameScene;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class SceneController {
    private static SceneController instance;
    private Client client;
    private Stage primaryStage;
    private GameScene currentGameScene;

    private SceneController() {}

    public static SceneController getInstance() {
        if (instance == null) {
            instance = new SceneController();
        }
        return instance;
    }

    public void init(Stage stage, Client client) {
        this.primaryStage = stage;
        this.client = client;
    }

    public void showLoginUI() {
        try {
            URL fxmlLocation = LoginController.class.getResource("/views/login.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Pane root = loader.load();

            LoginController loginController = loader.getController();
            loginController.setClient(client);

            Scene scene = new Scene(root);
            primaryStage.setTitle("1v1 Online Shooter");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();
        } catch (Exception e) {
            showErrorAlert("Lỗi hiển thị giao diện");
            e.printStackTrace();
        }
    }

    public void showLobbyUI() {
        try {
            URL fxmlLocation = LobbyController.class.getResource("/views/lobby.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Pane root = loader.load();

            LobbyController lobbyController = loader.getController();
            lobbyController.setClient(client);

            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            showErrorAlert("Lỗi hiển thị sảnh chờ");
            e.printStackTrace();
        }
    }

    public void showGameUI() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/game.fxml"));
            StackPane root = loader.load();

            // 1. Lấy GameController từ FXML
            GameController gameController = loader.getController();
            if (gameController != null) {
                gameController.setClient(client);
            }

            // 2. Khởi tạo GameScene với root và controller
            currentGameScene = new GameScene(root, gameController);

            // 3. Hiển thị Scene lên Stage
            primaryStage.setTitle("1v1 Online Shooter - In Game");
            primaryStage.setScene(currentGameScene.getScene());
            primaryStage.show();

            // 4. Bắt đầu Render & Game Loop
            currentGameScene.startLoop();
        } catch (Exception e) {
            showErrorAlert("Lỗi hiển thị giao diện game");
            e.printStackTrace();
        }
    }

    public GameScene getCurrentGameScene() {
        return currentGameScene;
    }

    public void showErrorAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
