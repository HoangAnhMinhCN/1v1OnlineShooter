package com.mycompany.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import javafx.application.Platform;

public class Client {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    private static SocketChannel tcpChannel;
    private DatagramChannel udpChannel;
    private Selector selector;
    private ConnectionCallback callback;

    public void setConnectionCallback(ConnectionCallback callback) {
        this.callback = callback;
    }

    public boolean connect() {
        try {
            selector = Selector.open();

            // 1. Khởi tạo TCP SocketChannel
            tcpChannel = SocketChannel.open();
            tcpChannel.configureBlocking(false);
            tcpChannel.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
            tcpChannel.register(selector, SelectionKey.OP_CONNECT);

            // 2. Khởi tạo UDP DatagramChannel
            udpChannel = DatagramChannel.open();
            udpChannel.configureBlocking(false);
            // Không bind port cố định để OS tự cấp phát port ngẫu nhiên
            udpChannel.register(selector, SelectionKey.OP_READ);

            // Vòng lặp lắng nghe phản hồi từ Server trên Event Loop Thread
            new Thread(this::eventLoop).start();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void eventLoop() {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            while (true) {
                selector.select();
                Iterator<SelectionKey> iter = selector.selectedKeys().iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (!key.isValid())
                        continue;

                    // --- HOÀN TẤT KẾT NỐI TCP ---
                    if (key.isConnectable()) {
                        try {
                            SocketChannel channel = (SocketChannel) key.channel();
                            if (channel.isConnectionPending()) {
                                channel.finishConnect();
                            }
                            System.out.println("[TCP] Đã kết nối thành công tới Server!");
                            channel.register(selector, SelectionKey.OP_READ);
                            notifySuccess();
                        } catch (Exception e) {
                            System.err.println("[Client] Kết nối thất bại (Server offline)!");
                            closeQuietly();
                            notifyFailure("Lỗi kết nối đến server");
                            return;
                        }
                    }
                    // --- ĐỌC DỮ LIỆU TỪ SERVER (TCP / UDP) ---
                    else if (key.isReadable()) {
                        buffer.clear();
                        if (key.channel() instanceof SocketChannel) {
                            int read = tcpChannel.read(buffer);
                            if (read > 0) {
                                buffer.flip();
                                System.out.println("[TCP Server response]: " + new String(buffer.array(), 0, read));
                            }
                        } else if (key.channel() instanceof DatagramChannel) {
                            udpChannel.receive(buffer);
                            buffer.flip();
                            System.out
                                    .println("[UDP Server response]: " + new String(buffer.array(), 0, buffer.limit()));
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Gửi tin nhắn TCP (Ví dụ: Đăng nhập, Chat)
    public void sendTcpMessage(String message) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap((message + "\n").getBytes());
            tcpChannel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Gửi gói tin UDP (Ví dụ: Tọa độ di chuyển)
    public void sendUdpData(String data) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(data.getBytes());
            SocketAddress target = new InetSocketAddress(SERVER_HOST, SERVER_PORT);
            udpChannel.send(buffer, target);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void notifySuccess() {
        if (callback != null) {
            Platform.runLater(() -> callback.onConnectSuccess());
        }
    }

    private void notifyFailure(String message) {
        if (callback != null) {
            // Bắt buộc dùng Platform.runLater vì UI chỉ được cập nhật trên JavaFX Application Thread
            Platform.runLater(() -> callback.onConnectFailure(message));
        }
    }

    private void closeQuietly() {
        try {
            if (tcpChannel != null)
                tcpChannel.close();
            if (udpChannel != null)
                udpChannel.close();
            if (selector != null)
                selector.close();
        } catch (IOException ignored) {
        }
    }

}
