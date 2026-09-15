package com.mycompany.server;

import java.sql.*;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class ClientHandler {

    // Lưu địa chỉ UDP hiện tại của từng người chơi để server broadcast trạng thái.
    private static final Map<Integer, SocketAddress> udpClients = new ConcurrentHashMap<>();

    public static void handleClientPacket(String message, Channel clientChannel) {
        handleClientPacket(message, clientChannel, null);
    }

    // Hàm này dùng chung cho TCP và UDP; senderAddress chỉ có giá trị với UDP.
    public static void handleClientPacket(String message, Channel clientChannel, SocketAddress senderAddress) {
        // Xử lý tin nhắn nhận được từ client
        try {
            String[] parts = message.split("\\|");
            String type = parts[0];
            switch (type) {
                case "LOGIN":
                    // Xử lý đăng nhập
                    login(parts, clientChannel);
                    
                    break;
                case "REQUEST_MATCH":
                    // Xử lý yêu cầu match
                    break;
                case "CHAT":
                    // Xử lý tin nhắn chat
                    break;

                case "MOVE":
                    // Packet MOVE chứa vị trí và góc quay của xe do client gửi lên.
                    if (clientChannel instanceof DatagramChannel && senderAddress != null) {
                        handleMove(parts, (DatagramChannel) clientChannel, senderAddress);
                    }
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void handleMove(String[] parts, DatagramChannel channel, SocketAddress sender)
            throws Exception {
        // Định dạng bắt buộc: MOVE|playerId|x|y|bodyAngle|turretAngle
        if (parts.length != 6) return;

        int playerId = Integer.parseInt(parts[1]);
        double x = Double.parseDouble(parts[2]);
        double y = Double.parseDouble(parts[3]);
        double bodyAngle = Double.parseDouble(parts[4]);
        double turretAngle = Double.parseDouble(parts[5]);

        // Cập nhật endpoint phòng trường hợp client đổi cổng UDP hoặc kết nối lại.
        udpClients.put(playerId, sender);

        String response = String.format(
                "MOVE|%d|%.2f|%.2f|%.2f|%.2f",
                playerId, x, y, bodyAngle, turretAngle);

        // Gửi vị trí cho tất cả người chơi khác, không gửi ngược lại người gửi.
        for (Map.Entry<Integer, SocketAddress> client : udpClients.entrySet()) {
            if (client.getKey() == playerId) continue;
            channel.send(ByteBuffer.wrap(response.getBytes()), client.getValue());
        }
    }
    private static void login(String[] parts, Channel clientChannel) {
        // Xử lý đăng nhập
        String username1 = parts[1];
        String password2 = parts[2];
        System.out.println("Đăng nhập: " + username1 + ", Mật khẩu: " + password2);
        try {
            Connection connection = DataBaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM player where name = ? AND password = ?");
            statement.setString(1, username1);
            statement.setString(2, password2);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                // Đăng nhập thành công
                System.out.println("login successed: " + username1);
                sendTcpResponse("LOGIN_SUCCESS", clientChannel);
            } else {
                // Đăng nhập thất bại
                System.out.println("Login failed" + username1);
                sendTcpResponse("LOGIN_FAILED", clientChannel);
            }
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private static void sendTcpResponse(String response, Channel clientChannel) {
        // Gửi phản hồi về client qua TCP
        if (clientChannel instanceof SocketChannel) {
            SocketChannel socketChannel = (SocketChannel) clientChannel;
            try {
                socketChannel.write(ByteBuffer.wrap((response + "\n").getBytes()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
