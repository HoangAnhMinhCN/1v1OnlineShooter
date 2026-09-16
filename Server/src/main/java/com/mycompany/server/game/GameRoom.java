package com.mycompany.server.game;

public class GameRoom {
    private int gameRoomId;
    private int idP1;
    private int idP2;

    public GameRoom(int gameRoomId, int idP1, int idP2) {
        this.gameRoomId = gameRoomId;
        this.idP1 = idP1;
        this.idP2 = idP2;
    }

    public int getIdP1() {
        return idP1;
    }

    public int getIdP2() {
        return idP2;
    }

    public int getGameRoomId() {
        return gameRoomId;
    }
}
