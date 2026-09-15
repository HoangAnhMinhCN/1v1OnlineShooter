package com.mycompany.client;

import java.nio.channels.Channel;

import com.mycompany.client.controller.LoginController;
import com.mycompany.client.controller.SceneController;

import javafx.application.Platform;

public class ServerHandler {
    public static void handleServerPacket(String message, Channel clientChannel) {
        
        try {
            String[] parts = message.split("\\|");
            String type = parts[0];
            switch (type) {
                case "LOGIN":
                    
                    
                    break;
                case "LOGIN_SUCCESS":
                    String playerId = parts[1];
                    Client.playerId = playerId; 
                    System.out.println("[ServerHandler] Đăng nhập thành công. Player ID: " + playerId);
                   
                    Platform.runLater(() -> {
                        SceneController.getInstance().showLobbyUI();
                    });
                    break;
                case "SHOOT":
                   System.out.println(message);
                    break;
                case "value2":
            
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    } 
}
