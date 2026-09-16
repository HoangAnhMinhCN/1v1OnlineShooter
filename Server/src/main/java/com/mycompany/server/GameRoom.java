package com.mycompany.server;

import java.util.UUID;
public class GameRoom {
    private String player1Id;   
    private String player2Id;
    private String roomId;
    public GameRoom(String player1Id, String player2Id) {
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.roomId = "Room_" + UUID.randomUUID(); // Tạo ID phòng ngẫu nhiên
    }
    public String getPlayer1Id() {
        return player1Id;
    }
    public void setPlayer1Id(String player1Id) {
        this.player1Id = player1Id;
    }
    public String getPlayer2Id() {
        return player2Id;
    }
    public void setPlayer2Id(String player2Id) {
        this.player2Id = player2Id;
    }
    public String getRoomId() {
        return roomId;
    }
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }   
    @Override 
    public String toString() {
        return "GameRoom [player1Id=" + player1Id + ", player2Id=" + player2Id + ", roomId=" + roomId + "]";
    }
}
