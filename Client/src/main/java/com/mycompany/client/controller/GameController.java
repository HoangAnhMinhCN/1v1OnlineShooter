package com.mycompany.client.controller;

import com.mycompany.client.Client;
import com.mycompany.client.game.GameScene;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

public class GameController {
    @FXML
    private Button closeChat;
    @FXML
    private Button openChat;
    @FXML
    private VBox chatArea;
    @FXML
    private ListView<String> messageArea;
    @FXML
    private TextField inputMessage;

    private Client client;

    public void setClient(Client client) {
        this.client = client;
    }

    @FXML
    public void handleOpenChat() {
        openChat.setOnMouseClicked(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                chatArea.setVisible(true);
                openChat.setVisible(false);
            }
        });
    }

    @FXML
    public void handleCloseChat() {
        closeChat.setOnMouseClicked(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent e) {
                chatArea.setVisible(false);
                openChat.setVisible(true);
            }
        });
    }

    public void onShootPressed() {
        // 1. Lấy GameScene hiện tại
        GameScene gameScene = SceneController.getInstance().getCurrentGameScene();

        if (gameScene != null) {
            // Tạo đạn cục bộ ngay lập tức cho mượt
            gameScene.spawnBullet(gameScene.getLocalTank());

            // 2. Gửi gói tin thông báo bắn lên Server (nếu đánh Online)
            if (client != null) {
                // client.sendData("SHOOT|...");
                client.sendTcpMessage("SHOOT|x|y");
            }
        }
    }

    public void onInputChanged(boolean moveUp, boolean moveDown, boolean moveLeft, boolean moveRight) {

    }

    public void onTurretAngleChanged(double turretAngle) {

    }
}
