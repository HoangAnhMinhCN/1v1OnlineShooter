package com.mycompany.client.controller;

import com.mycompany.client.Client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class LobbyController {
    @FXML private Button matchBtn;
    @FXML private Label lbPlayerName;

    public LobbyController() {
    }

    @FXML 
    public void handleMatch() {
        //SceneController.getInstance().showGameUI();
        System.out.println("[LobbyController] Gửi yêu cầu match đến server.");
        Client.sendTcpMessage("MATCH_REQUEST|" + Client.getInstance().getPlayerId() + "|" + Client.getInstance().getPlayerName());
    }

    public void initNamePlayer(Client client) {
        lbPlayerName.setText(lbPlayerName.getText() + " " + client.getPlayerName());
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
