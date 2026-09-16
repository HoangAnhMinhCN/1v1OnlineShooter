package com.mycompany.server;

import java.net.SocketAddress;
import java.nio.channels.*;
public class Player {
    private SocketChannel tcpChannel;
    private SocketAddress udpAddress;
    private String id;

    public Player(SocketChannel tcpChannel, String playerId) {
        this.tcpChannel = tcpChannel;
        this.id = playerId;
    }
    public Player(SocketChannel tcpChannel, SocketAddress udpAddress, String playerId) {
        this.tcpChannel = tcpChannel;
        this.udpAddress = udpAddress;
        this.id = playerId;
    }
    public SocketChannel getTcpChannel() {
        return tcpChannel;
    }
    public void setTcpChannel(SocketChannel tcpChannel) {
        this.tcpChannel = tcpChannel;
    }
    public SocketAddress getUdpAddress() {
        return udpAddress;
    }
    public void setUdpAddress(SocketAddress udpAddress) {
        this.udpAddress = udpAddress;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    @Override
    public String toString() {
        return "Player [tcpChannel=" + tcpChannel + ", udpAddress=" + udpAddress + ", id=" + id + "]";
    }
    
    
    

}
