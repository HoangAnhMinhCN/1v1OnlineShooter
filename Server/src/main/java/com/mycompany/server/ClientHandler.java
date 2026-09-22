package com.mycompany.server;

import java.sql.*;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler {

    // Lưu địa chỉ UDP hiện tại của từng người chơi để server broadcast trạng thái.
    private static final Map<String, SocketAddress> udpClients = new ConcurrentHashMap<>();

    public static void handleClientPacket(String message, Channel clientChannel) {
        handleClientPacket(message, clientChannel, null);
    }

    // Hàm này dùng chung cho TCP và UDP; senderAddress chỉ có giá trị với UDP.
    public static void handleClientPacket(String message, Channel clientChannel, SocketAddress senderAddress) {
        // Xử lý tin nhắn nhận được từ client
        try {
            String[] parts = message.split("\\|");
            String type = parts[0];
            switch (type) {
                case "LOGIN":
                    // Xử lý đăng nhập
                    login(parts, clientChannel);
                    break;
                case "UDP_ADDRESS":
                    Server.players.get(parts[1]).setUdpAddress(senderAddress);
                    System.out.println("Đã nhận địa chỉ UDP từ client: " + senderAddress);
                    for (Map.Entry<String, Player> player : Server.players.entrySet()) {
                        System.out.println("PlayerId: " + player.getKey() + ", UDP Address: " + player.getValue());
                    }
                    break;
                case "MATCH_REQUEST":
                    // Xử lý yêu cầu match
                    if (Server.players.containsKey(parts[1])) {
                        Server.matchMakingQueue.offer(parts[1]); // Thêm client vào danh sách chờ match
                        System.out.println(
                                "Client " + parts[1] + " đã yêu cầu match. Danh sách chờ: " + Server.matchMakingQueue);
                        if (Server.matchMakingQueue.size() >= 2) {
                            String player1Id = Server.matchMakingQueue.poll();
                            String player2Id = Server.matchMakingQueue.poll();

                            Player p1 = Server.players.get(player1Id);
                            Player p2 = Server.players.get(player2Id);

                            // Tạo phòng chơi mới
                            GameRoom newRoom = new GameRoom(player1Id, player2Id);
                            Server.gameRooms.add(newRoom);
                            System.out.println("Tạo phòng chơi mới giữa " + player1Id + " và " + player2Id);
                            // Gửi thông báo cho cả hai client về việc bắt đầu trận đấu
                            ClientHandler.sendTcpResponse(
                                    "MATCH_FOUND|" + player2Id + "|" + newRoom.getRoomId() + "|1" + "|"
                                            + p2.getPlayerName(),
                                    Server.players.get(player1Id).getTcpChannel());
                            ClientHandler.sendTcpResponse(
                                    "MATCH_FOUND|" + player1Id + "|" + newRoom.getRoomId() + "|2" + "|"
                                            + p1.getPlayerName(),
                                    Server.players.get(player2Id).getTcpChannel());
                            Server.matchMakingQueue.remove(player1Id);
                            Server.matchMakingQueue.remove(player2Id);
                            for (GameRoom room : Server.gameRooms) {
                                System.out.println(room);
                            }
                        }
                    } else {
                        System.out.println("Client " + parts[1] + " không tồn tại trong danh sách người chơi.");
                    }

                    break;

                case "CHAT":
                    // Xử lý tin nhắn chat
                    break;

                case "MOVE":
                    // Packet MOVE chứa vị trí và góc quay của xe do client gửi lên.
                    if (clientChannel instanceof DatagramChannel && senderAddress != null) {
                        handleMove(parts, (DatagramChannel) clientChannel, senderAddress);
                    }
                    break;
                case "SHOOT":
                    // Chỉ xử lý SHOOT nếu gói tin đến từ kênh UDP và có địa chỉ người gửi.
                    if (clientChannel instanceof DatagramChannel && senderAddress != null) {
                        // Chuyển gói tin đến hàm xử lý bắn đạn.
                        handleShoot(parts, (DatagramChannel) clientChannel, senderAddress);
                    }
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void handleShoot(String[] parts, DatagramChannel channel, SocketAddress sender)
            throws Exception {
        // Gói có dạng: SHOOT|gameRoomId|playerId|x|y|turretAngle.
        if (parts.length != 6)
            return;
        String gameRoomId = parts[1];
        String playerId = parts[2];
        double x = Double.parseDouble(parts[3]);
        double y = Double.parseDouble(parts[4]);
        double angle = Double.parseDouble(parts[5]);

        String anotherPlayerId = null;
        for (GameRoom gameRoom : Server.getGameRooms()) {
            if (gameRoomId.equals(gameRoom.getRoomId())) {
                if (playerId.equals(gameRoom.getPlayer1Id()))
                    anotherPlayerId = gameRoom.getPlayer2Id();
                else
                    anotherPlayerId = gameRoom.getPlayer1Id();
                break;
            }
        }

        // Đăng ký endpoint UDP đầu tiên của player nếu chưa có.
        udpClients.putIfAbsent(playerId, sender);

        // Bỏ qua gói nếu playerId đang bị một endpoint khác sở hữu.
        if (!sender.equals(udpClients.get(playerId)))
            return;

        // String response = String.format("SHOOT|%s|%.2f|%.2f|%.2f", playerId, x, y,
        // angle);

        // for (Map.Entry<String, SocketAddress> client : udpClients.entrySet()) {
        // if (client.getKey() == anotherPlayerId)
        // channel.send(ByteBuffer.wrap(response.getBytes()), client.getValue());
        // }

        if (anotherPlayerId == null)
            return;

        // 3. Lấy SocketAddress của đối thủ (Ưu tiên lấy từ udpClients, nếu không có thì
        // lấy từ Server.players)
        SocketAddress targetAddress = udpClients.get(anotherPlayerId);
        if (targetAddress == null && Server.players.containsKey(anotherPlayerId)) {
            targetAddress = Server.players.get(anotherPlayerId).getUdpAddress();
        }

        // Nếu đối thủ đã mở cổng UDP, tiến hành gửi gói tin sang
        if (targetAddress != null) {
            String response = String.format("SHOOT|%s|%.2f|%.2f|%.2f", playerId, x, y, angle);
            channel.send(ByteBuffer.wrap(response.getBytes()), targetAddress);
            System.out.println("[Server] Đã chuyển sự kiện SHOOT từ " + playerId + " sang đối thủ " + anotherPlayerId);
        } else {
            System.out.println("[Server] Đối thủ " + anotherPlayerId + " chưa cấu hình địa chỉ UDP!");
        }
    }

    private static void handleMove(String[] parts, DatagramChannel channel, SocketAddress sender)
            throws Exception {
        // Định dạng bắt buộc: MOVE|gameRoomId|playerId|x|y|bodyAngle|turretAngle
        if (parts.length != 7)
            return;

        String gameRoomId = parts[1];
        String playerId = parts[2];
        double x = Double.parseDouble(parts[3]);
        double y = Double.parseDouble(parts[4]);
        double bodyAngle = Double.parseDouble(parts[5]);
        double turretAngle = Double.parseDouble(parts[6]);

        String anotherPlayerId = null;
        for (GameRoom gameRoom : Server.getGameRooms()) {
            if (gameRoomId.equals(gameRoom.getRoomId())) {
                if (playerId.equals(gameRoom.getPlayer1Id()))
                    anotherPlayerId = gameRoom.getPlayer2Id();
                else
                    anotherPlayerId = gameRoom.getPlayer1Id();
                break;
            }
        }

        // Cập nhật endpoint phòng trường hợp client đổi cổng UDP hoặc kết nối lại.
        udpClients.put(playerId, sender);

        // String response = String.format(
        // "MOVE|%s|%.2f|%.2f|%.2f|%.2f",
        // playerId, x, y, bodyAngle, turretAngle);

        // // Gửi vị trí cho tất cả người chơi khác, không gửi ngược lại người gửi.
        // for (Map.Entry<String, SocketAddress> client : udpClients.entrySet()) {
        // if (client.getKey() == anotherPlayerId)
        // channel.send(ByteBuffer.wrap(response.getBytes()), client.getValue());
        // }

        if (anotherPlayerId == null)
            return;

        // 3. Lấy SocketAddress của đối thủ (Ưu tiên lấy từ udpClients, nếu không có thì
        // lấy từ Server.players)
        SocketAddress targetAddress = udpClients.get(anotherPlayerId);
        if (targetAddress == null && Server.players.containsKey(anotherPlayerId)) {
            targetAddress = Server.players.get(anotherPlayerId).getUdpAddress();
        }

        // Nếu đối thủ đã mở cổng UDP, tiến hành gửi gói tin sang
        if (targetAddress != null) {
            String response = String.format(
                    "MOVE|%s|%.2f|%.2f|%.2f|%.2f",
                    playerId, x, y, bodyAngle, turretAngle);
            channel.send(ByteBuffer.wrap(response.getBytes()), targetAddress);
        }
    }

    private static void login(String[] parts, Channel clientChannel) {
        // Xử lý đăng nhập
        String username1 = parts[1];
        String password2 = parts[2];
        System.out.println("Đăng nhập: " + username1 + ", Mật khẩu: " + password2);
        try {
            Connection connection = DataBaseManager.getConnection();
            PreparedStatement statement = connection
                    .prepareStatement("SELECT * FROM player where name = ? AND password = ?");
            statement.setString(1, username1);
            statement.setString(2, password2);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                // Đăng nhập thành công
                System.out.println("login successed: " + username1);
                String id = resultSet.getString("id");
                String name = resultSet.getString("name");

                sendTcpResponse("LOGIN_SUCCESS|" + id + "|" + name, clientChannel);
                Server.players.put(id, new Player((SocketChannel) clientChannel, id, name));

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
