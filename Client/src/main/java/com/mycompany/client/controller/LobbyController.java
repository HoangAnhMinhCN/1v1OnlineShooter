package com.mycompany.client.controller;

import com.mycompany.client.Client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class LobbyController {
    @FXML private Button matchBtn;

    private Client client;
    private Stage primaryStage;

    public void setClient(Client client) {
        this.client = client;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public LobbyController(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public LobbyController() {
    }

    @FXML 
    public void handleMatch() {
        //SceneController.getInstance().showGameUI();
        System.out.println("[LobbyController] Gửi yêu cầu match đến server.");
        Client.sendTcpMessage("REQUEST_MATCH");
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
