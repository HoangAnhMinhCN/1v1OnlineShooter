package com.mycompany.client;

import java.nio.channels.Channel;

public class ServerHandler {
    public static void handleServerPacket(String message, Channel clientChannel) {
        
        try {
            String[] parts = message.split("\\|");
            String type = parts[0];
            switch (type) {
                case "LOGIN":
                    
                    
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
