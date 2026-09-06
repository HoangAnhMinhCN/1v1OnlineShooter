package com.mycompany.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class Server {
    private static final int PORT = 12345;
    private Selector selector;
    private ServerSocketChannel tcpServerChannel;
    private DatagramChannel udpServerChannel;

    public Server() {
        try {
            // 1. Khởi tạo Selector
            selector = Selector.open();

            // 2. Khởi tạo TCP Channel (Non-blocking)
            tcpServerChannel = ServerSocketChannel.open();
            tcpServerChannel.configureBlocking(false);
            tcpServerChannel.bind(new InetSocketAddress(PORT));
            tcpServerChannel.register(selector, SelectionKey.OP_ACCEPT);

            // 3. Khởi tạo UDP Channel (Non-blocking)
            udpServerChannel = DatagramChannel.open();
            udpServerChannel.configureBlocking(false);
            udpServerChannel.bind(new InetSocketAddress(PORT));
            udpServerChannel.register(selector, SelectionKey.OP_READ);

            System.out.println("[NIO Server] Đã khởi động trên port " + PORT);
            startLoop();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startLoop() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        while (true) {
            // Dừng chờ cho đến khi có ít nhất 1 sự kiện I/O sẵn sàng
            selector.select();

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();

            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove(); // Bắt buộc xóa key sau khi lấy ra xử lý

                if (!key.isValid())
                    continue;

                // --- XỬ LÝ TCP ACCEPT ---
                if (key.isAcceptable()) {
                    ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                    SocketChannel clientChannel = serverChannel.accept();
                    clientChannel.configureBlocking(false);
                    // Đăng ký client TCP này vào Selector để chờ đọc dữ liệu
                    clientChannel.register(selector, SelectionKey.OP_READ);
                    System.out.println("[TCP] Client mới kết nối: " + clientChannel.getRemoteAddress());
                }

                // --- XỬ LÝ ĐỌC DỮ LIỆU (TCP HOẶC UDP) ---
                else if (key.isReadable()) {
                    Channel channel = key.channel();

                    // Case A: Đọc dữ liệu từ TCP
                    if (channel instanceof SocketChannel) {
                        SocketChannel clientChannel = (SocketChannel) channel;
                        buffer.clear();

                        try {
                            int bytesRead = clientChannel.read(buffer);

                            if (bytesRead == -1) { // Client ngắt kết nối
                                System.out.println("[TCP] Client đã ngắt kết nối: " + clientChannel.getRemoteAddress());
                                clientChannel.close();
                            } else if (bytesRead > 0) {
                                buffer.flip();
                                byte[] data = new byte[buffer.remaining()];
                                buffer.get(data);
                                String msg = new String(data).trim();
                                System.out.println("[TCP Received]: " + msg);
                            }
                        } catch (Exception e) {
                            System.out.println(
                                    "[TCP] Client đã ngắt kết nối đột ngột: " + clientChannel.getRemoteAddress());
                            disconnectClient(clientChannel);
                        }
                    }
                    // Case B: Đọc gói tin từ UDP
                    else if (channel instanceof DatagramChannel) {
                        DatagramChannel datagramChannel = (DatagramChannel) channel;
                        buffer.clear();
                        SocketAddress clientAddress = datagramChannel.receive(buffer);

                        if (clientAddress != null) {
                            try {
                                buffer.flip();
                                byte[] data = new byte[buffer.remaining()];
                                buffer.get(data);
                                String msg = new String(data).trim();
                                System.out.println("[UDP Received] Từ " + clientAddress + ": " + msg);
                            } catch (Exception e) {
                                System.out.println(
                                        "[TCP] Client đã ngắt kết nối đột ngột: " + datagramChannel.getRemoteAddress());
                                disconnectClient(datagramChannel);
                            }
                        }
                    }
                }
            }
        }
    }

    private void disconnectClient(Channel clientChannel) {
        try {
            // 1. Đóng channel
            clientChannel.close();

            // 2. Xóa client khỏi danh sách/map quản lý của Server (nếu có)
            // clients.remove(clientChannel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Server();
    }
}
