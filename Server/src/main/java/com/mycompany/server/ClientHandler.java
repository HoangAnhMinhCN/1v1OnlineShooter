package com.mycompany.server;

import java.sql.*;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mycompany.server.game.ServerTank;

public class ClientHandler {

    // Lưu địa chỉ UDP hiện tại của từng người chơi để server broadcast trạng thái.
    private static final Map<String, SocketAddress> udpClients = new ConcurrentHashMap<>();

    public static void handleClientPacket(String message, Channel clientChannel) {
        handleClientPacket(message, clientChannel, null);
    }

    // hàm gửi packet cho udpAdress trong phòng
    public static String buildState(GameRoom room) {
        ServerTank p1 = Server.gameEngine.getTank(room.getPlayer1Id());
        ServerTank p2 = Server.gameEngine.getTank(room.getPlayer2Id());

        return String.format(
                "STATE|%s|%d|%d|%d|%.2f|%.2f|%.2f|%.2f|%.2f|%.2f|%.2f|%.2f",
                room.getRoomId(),
                Server.gameEngine.getServerTick(),
                p1.getLastInputSeq(),
                p2.getLastInputSeq(),
                p1.getX(), p1.getY(), p1.getBodyAngle(), p1.getTurretAngle(),
                p2.getX(), p2.getY(), p2.getBodyAngle(), p2.getTurretAngle());
    }

    private static void handleInput(String[] parts) {
        // INPUT|roomId|playerId|seq|keys|turretAngle
        if (parts.length != 6)
            return;

        try {
            String roomId = parts[1];
            String playerId = parts[2];
            long seq = Long.parseLong(parts[3]);
            int keys = Integer.parseInt(parts[4]);
            double turretAngle = Double.parseDouble(parts[5]);

            if (keys < 0 || keys > 15)
                return;

            Server.gameEngine.applyInput(roomId, playerId, seq, keys, turretAngle);
        } catch (NumberFormatException ignored) {
            // packet lỗi: bỏ
        }
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
                            // Tạo phòng chơi mới
                            GameRoom newRoom = new GameRoom(player1Id, player2Id);
                            Server.gameRooms.add(newRoom);
                            Server.gameEngine.createMatch(newRoom);
                            System.out.println("Tạo phòng chơi mới giữa " + player1Id + " và " + player2Id);
                            // Gửi thông báo cho cả hai client về việc bắt đầu trận đấu
                            ClientHandler.sendTcpResponse("MATCH_FOUND|" + player2Id + "|" + newRoom.getRoomId() + "|1",
                                    Server.players.get(player1Id).getTcpChannel());
                            ClientHandler.sendTcpResponse("MATCH_FOUND|" + player1Id + "|" + newRoom.getRoomId() + "|2",
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
                case "REQUEST_MATCH":
                    // Xử lý yêu cầu match
                    break;
                case "CHAT":
                    // Xử lý tin nhắn chat
                    break;

                // case "MOVE":
                // // Packet MOVE chứa vị trí và góc quay của xe do client gửi lên.
                // if (clientChannel instanceof DatagramChannel && senderAddress != null) {
                // handleMove(parts, (DatagramChannel) clientChannel, senderAddress);
                // }
                // break;
                case "INPUT":
                    if (clientChannel instanceof DatagramChannel && senderAddress != null) {
                        handleInput(parts);
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

    /**
     * Nhận input thay vì nhận tọa độ. GameEngine sẽ là nơi duy nhất mô phỏng và
     * quyết định vị trí chính thức của xe tăng.
     */
    private static void handleInput(String[] parts, SocketAddress sender) {
        if (parts.length != 6) {
            return;
        }

        try {
            String roomId = parts[1];
            String playerId = parts[2];
            long sequence = Long.parseLong(parts[3]);
            int keys = Integer.parseInt(parts[4]);
            double turretAngle = Double.parseDouble(parts[5]);

            // Chúng ta chấp nhận 4 bit điều khiển UP/DOWN/LEFT/RIGHT.
            if (sequence < 0 || keys < 0 || keys > 15 || !Double.isFinite(turretAngle)) {
                return;
            }

            if (!Server.players.containsKey(playerId)) {
                return;
            }

            boolean accepted = Server.gameEngine.applyInput(
                    roomId, playerId, sequence, keys, turretAngle);

            if (accepted) {
                // Tạm thời gắn endpoint từ packet INPUT.
                // Bước session sau sẽ xác thực sessionId trước khi
                // cho phép cập nhật endpoint này.
                udpClients.put(playerId, sender);
                Server.players.get(playerId).setUdpAddress(sender);
            }
        } catch (NumberFormatException ignored) {
            // Datagram lỗi: bỏ qua, không để nó ảnh hưởng đến event loop.
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

                sendTcpResponse("LOGIN_SUCCESS|" + id, clientChannel);
                Server.players.put(id, new Player((SocketChannel) clientChannel, id));

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
