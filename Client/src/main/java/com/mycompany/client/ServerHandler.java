package com.mycompany.client;

import com.mycompany.client.game.Tank;
import com.mycompany.client.game.GameScene;
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
                    Client.getInstance().setIdPlayer(parts[1]);
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

                // tank di chuyen
                case "MOVE":
                    // parts = ["MOVE", "1", "120.50", "340.00", "90.00", "135.00"]
                    int playerId = Integer.parseInt(parts[1]); // Id nguoi nguoi goi tin
                    double x = Double.parseDouble(parts[2]); // toa do x
                    double y = Double.parseDouble(parts[3]); // toa do y
                    double bodyAngle = Double.parseDouble(parts[4]); // goc than xe
                    double turretAngle = Double.parseDouble(parts[5]); // goc nòng pháo

                    // chay trn javafx thread de cap nhat ui cho object

                    Platform.runLater(() -> {
                        // Lay Tank doi thu
                        Tank enemy = GameScene.getInstance().getEnemyTank(playerId);
                        if (enemy != null) {
                            enemy.setPosition(x, y); // cap nhat toa do tank sicj
                            enemy.setTurretAngle(turretAngle); // cap nhat huong phao
                            enemy.setAngle(bodyAngle); // cap nhat huong than xe
                        }

                    });
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
