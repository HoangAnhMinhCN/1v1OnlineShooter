package com.mycompany.client.controller;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

public class GameController {
    @FXML private Button closeChat;
    @FXML private Button openChat;
    @FXML private VBox chatArea;
    @FXML private ListView<String> messageArea;
    @FXML private TextField inputMessage;

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
}
