package com.mycompany.client.controller;

import com.mycompany.client.Client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {
    @FXML
    private Button loginBtn;
    @FXML 
    private TextField username;
    @FXML
    private TextField password;

    private Client client;

    public LoginController() {}

    public void setClient(Client client) {
        this.client = client;
    }

    @FXML
    public void handleLogin() {
        // SceneController.getInstance().showLobbyUI();
        client.sendTcpMessage("LOGIN|"+username.getText()+"|"+password.getText());
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
