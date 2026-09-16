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
                    // Đăng nhập thành công → chuyển sang màn hình Lobby
                    Platform.runLater(() -> SceneController.getInstance().showLobbyUI());
                    Client.setPlayerId(parts[1]); // Lưu ID người chơi vào Client
                    Client.sendUdpData("UDP_ADDRESS|" + Client.getInstance().getPlayerId()); // Gửi địa chỉ UDP của client lên server
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
                    // Kiểm tra gói có đủ 5 trường: SHOOT, ID, X, Y và góc bắn.
                    if (parts.length == 5) {
                        // Đọc ID của người chơi đã bắn.
                        String playerId = parts[1];
                        // Đọc tọa độ X của viên đạn.
                        double x = Double.parseDouble(parts[2]);
                        // Đọc tọa độ Y của viên đạn.
                        double y = Double.parseDouble(parts[3]);
                        // Đọc góc bắn của viên đạn.
                        double angle = Double.parseDouble(parts[4]);
                        // Đưa việc tạo đạn lên JavaFX Application Thread.
                        Platform.runLater(() -> GameScene.getInstance().spawnBullet(x, y, angle, playerId));
                    }
                    break;

                // tank di chuyen
                case "MOVE":
                    // parts = ["MOVE", "1", "120.50", "340.00", "90.00", "135.00"]
                    String playerId = parts[1]; // Id nguoi gui goi tin
                    double x = Double.parseDouble(parts[2]); // toa do x
                    double y = Double.parseDouble(parts[3]); // toa do y
                    double bodyAngle = Double.parseDouble(parts[4]); // goc than xe
                    double turretAngle = Double.parseDouble(parts[5]); // goc nòng pháo

                    // chay tren javafx thread de cap nhat ui cho object

                    Platform.runLater(() -> {
                        // Lay Tank doi thu
                        // Tìm đúng tank theo ID trong packet, tránh cập nhật nhầm tank khác.
                        Tank enemy = GameScene.getInstance().getTank(playerId);
                        if (enemy != null) {
                            // Chỉ đặt vị trí mục tiêu; Tank sẽ nội suy để chuyển động mượt.
                            enemy.setTargetPosition(x, y);
                            enemy.setTurretAngle(turretAngle); // cap nhat huong phao
                            enemy.setAngle(bodyAngle); // cap nhat huong than xe
                        }

                    });
                    break;
                
                case "MATCH_FOUND":
                    Client.getInstance().setGameRoomId(parts[1]);
                    Platform.runLater(() -> SceneController.getInstance().showGameUI());
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
