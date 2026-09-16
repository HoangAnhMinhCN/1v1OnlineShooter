package com.mycompany.server;

import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

public class ClientSession {
    private final int playerId;
    private final SocketChannel tcpChannel;
    private SocketAddress udpAddress; // Cập nhật khi nhận gói UDP đầu tiên từ Client

    public ClientSession(int playerId, SocketChannel tcpChannel) {
        this.playerId = playerId;
        this.tcpChannel = tcpChannel;
    }

    // --- GETTERS & SETTERS ---

    public int getPlayerId() {
        return playerId;
    }

    public SocketChannel getTcpChannel() {
        return tcpChannel;
    }

    public SocketAddress getUdpAddress() {
        return udpAddress;
    }

    public void setUdpAddress(SocketAddress udpAddress) {
        this.udpAddress = udpAddress;
    }
}
