package com.mycompany.server;

import java.nio.channels.Channel;

public class ClientHandler {

    public static void handleClientPacket(String message, Channel clientChannel) {
        // Xử lý tin nhắn nhận được từ client
        try {
            String[] parts = message.split("\\|");
            String type = parts[0];
            switch (type) {
                case "LOGIN":
                    login();
                    // Xử lý đăng nhập
                    break;
                case "REQUEST_MATCH":
                    // Xu ly yêu cầu match
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
    private static void login() {
        // Xử lý đăng nhập
        // Connection connectionManager = DataBaseManager.getConnection();
    }
}
