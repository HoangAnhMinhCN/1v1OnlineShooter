package com.mycompany.client;

import java.nio.channels.Channel;

import com.mycompany.client.controller.SceneController;

import javafx.application.Platform;
import javafx.scene.control.Alert;

public class ServerHandler {
    public static void handleServerPacket(String message, Channel clientChannel) {
        
        try {
            String[] parts = message.split("\\|");
            String type = parts[0];
            switch (type) {
                case "LOGIN_SUCCESS":
                    // Đăng nhập thành công → chuyển sang màn hình Lobby
                    Platform.runLater(() -> SceneController.getInstance().showLobbyUI());
                    break;

                case "LOGIN_FAILED":
                    // Đăng nhập thất bại → hiển thị thông báo lỗi
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Đăng nhập thất bại");
                        alert.setHeaderText(null);
                        alert.setContentText("Tên đăng nhập hoặc mật khẩu không đúng!");
                        alert.showAndWait();
                    });
                    break;

                case "SHOOT":
                    System.out.println(message);
                    break;

                default:
                    System.out.println("[ServerHandler] Unknown packet: " + message);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    } 
}
