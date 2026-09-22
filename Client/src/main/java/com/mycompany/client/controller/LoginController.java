package com.mycompany.client.controller;

import com.mycompany.client.Client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {
    @FXML
    private Button loginBtn;
    @FXML 
    private TextField username;
    @FXML
    private TextField password;

    public LoginController() {}


    @FXML
    public void handleLogin() {
        Client.sendTcpMessage("LOGIN|"+username.getText()+"|"+password.getText());
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
