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
                    Client.getInstance().setPlayerId(parts[1]); // Lưu ID người chơi vào Client
                    Client.getInstance().setPlayerName(parts[2]);
                    // Đăng nhập thành công → chuyển sang màn hình Lobby
                    Platform.runLater(() -> SceneController.getInstance().showLobbyUI());
                    Client.sendUdpData("UDP_ADDRESS|" + Client.getInstance().getPlayerId()); // Gửi địa chỉ UDP của
                                                                                             // client lên server
                    break;
                case "":
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
                    ShootingPacketHandler.handle(parts);
                    break;

                case "STATE":
                    StatePacketHandler.handle(parts);
                    break;

                case "MATCH_FOUND":
                    Client.getInstance().setGameRoomId(parts[2]);
                    Client.getInstance().setAnotherPlayerId(parts[1]);
                    Client.getInstance().setMyNumber(Integer.parseInt(parts[3]));
                    Platform.runLater(() -> SceneController.getInstance().showGameUI(parts[4]));
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
