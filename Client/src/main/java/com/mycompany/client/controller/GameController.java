package com.mycompany.client.controller;

import com.mycompany.client.Client;
import com.mycompany.client.GamePacketSender;
import com.mycompany.client.game.GameScene;
import com.mycompany.client.game.Tank;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
    @FXML
    private Label namePlayer1;
    @FXML
    private Label namePlayer2;

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

    public void initPlayerNames(String nameP1, String nameP2) {
        if (Client.getInstance().getMyNumber() == 1) {
            namePlayer1.setText(nameP1);
            namePlayer1.setStyle("-fx-font-weight: bold;");
            namePlayer2.setText(nameP2);
        } else {
            namePlayer2.setText(nameP1);
            namePlayer2.setStyle("-fx-font-weight: bold;");
            namePlayer1.setText(nameP2);
        }
    }

    public void onShootPressed() {
        // 1. Lấy GameScene hiện tại
        GameScene gameScene = SceneController.getInstance().getCurrentGameScene();

        if (gameScene != null) {
            // Tạo đạn cục bộ ngay lập tức cho mượt
            gameScene.spawnBullet(gameScene.getLocalTank());

            // 2. Gửi gói tin thông báo bắn lên Server
            if (client != null) {
                // Lấy tank đang được người chơi điều khiển.
                Tank tank = gameScene.getLocalTank();
                GamePacketSender.sendShoot(client, tank);
            }
        }
    }
}
