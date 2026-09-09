package com.mycompany.server;

import java.sql.*;
import java.nio.channels.*;
import java.nio.ByteBuffer;
public class ClientHandler {

    public static void handleClientPacket(String message, Channel clientChannel) {
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

                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
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
