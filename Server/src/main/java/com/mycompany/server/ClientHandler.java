package com.mycompany.server;

import com.mycompany.server.game.ServerTank;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Handles TCP lobby packets and UDP gameplay packets. */
public final class ClientHandler {
    private static final Map<String, SocketAddress> udpClients = new ConcurrentHashMap<>();

    private ClientHandler() {
    }

    public static void handleClientPacket(String message, Channel clientChannel) {
        handleClientPacket(message, clientChannel, null);
    }

    public static void handleClientPacket(String message, Channel clientChannel, SocketAddress senderAddress) {
        if (message == null || message.isBlank()) {
            return;
        }

        String[] parts = message.split("\\|");
        try {
            switch (parts[0]) {
                case "LOGIN" -> login(parts, clientChannel);
                case "UDP_ADDRESS" -> registerUdpAddress(parts, senderAddress);
                case "MATCH_REQUEST" -> requestMatch(parts);
                case "INPUT" -> {
                    if (clientChannel instanceof DatagramChannel && senderAddress != null) {
                        handleInput(parts, senderAddress);
                    }
                }
                case "SHOOT" -> {
                    if (clientChannel instanceof DatagramChannel && senderAddress != null) {
                        handleShoot(parts, (DatagramChannel) clientChannel, senderAddress);
                    }
                }
                default -> System.out.println("[Server] Unknown packet type: " + parts[0]);
            }
        } catch (Exception e) {
            System.err.println("[Server] Could not process packet: " + e.getMessage());
        }
    }

    private static void registerUdpAddress(String[] parts, SocketAddress senderAddress) {
        if (parts.length != 2 || senderAddress == null) {
            return;
        }

        Player player = Server.players.get(parts[1]);
        if (player != null) {
            player.setUdpAddress(senderAddress);
            udpClients.put(parts[1], senderAddress);
        }
    }

    private static void requestMatch(String[] parts) {
        if (parts.length != 2 || !Server.players.containsKey(parts[1])) {
            return;
        }

        String playerId = parts[1];
        if (!Server.matchMakingQueue.contains(playerId)) {
            Server.matchMakingQueue.offer(playerId);
        }
        if (Server.matchMakingQueue.size() < 2) {
            return;
        }

        String player1Id = Server.matchMakingQueue.poll();
        String player2Id = Server.matchMakingQueue.poll();
        if (player1Id == null || player2Id == null || player1Id.equals(player2Id)) {
            return;
        }

        GameRoom room = new GameRoom(player1Id, player2Id);
        Server.gameEngine.createMatch(room);

        sendTcpResponse("MATCH_FOUND|" + player2Id + "|" + room.getRoomId() + "|1",
                Server.players.get(player1Id).getTcpChannel());
        sendTcpResponse("MATCH_FOUND|" + player1Id + "|" + room.getRoomId() + "|2",
                Server.players.get(player2Id).getTcpChannel());
    }

    private static void handleInput(String[] parts, SocketAddress sender) {
        // INPUT|roomId|playerId|seq|keys|turretAngle
        if (parts.length != 6) {
            return;
        }

        try {
            String roomId = parts[1];
            String playerId = parts[2];
            long sequence = Long.parseLong(parts[3]);
            int keys = Integer.parseInt(parts[4]);
            double turretAngle = Double.parseDouble(parts[5]);

            if (sequence < 0 || keys < 0 || keys > 15 || !Double.isFinite(turretAngle)
                    || !Server.players.containsKey(playerId)) {
                return;
            }

            if (Server.gameEngine.applyInput(roomId, playerId, sequence, keys, turretAngle)) {
                udpClients.put(playerId, sender);
                Server.players.get(playerId).setUdpAddress(sender);
            }
        } catch (NumberFormatException ignored) {
            // Invalid datagram: discard it without stopping the server event loop.
        }
    }

    /** Builds one authoritative snapshot for the two players in a room. */
    public static String buildState(GameRoom room) {
        ServerTank p1 = Server.gameEngine.getTank(room.getPlayer1Id());
        ServerTank p2 = Server.gameEngine.getTank(room.getPlayer2Id());
        if (p1 == null || p2 == null) {
            return null;
        }

        return String.format(
                "STATE|%s|%d|%d|%d|%.2f|%.2f|%.2f|%.2f|%.2f|%.2f|%.2f|%.2f",
                room.getRoomId(),
                Server.gameEngine.getServerTick(),
                p1.getLastInputSeq(),
                p2.getLastInputSeq(),
                p1.getX(), p1.getY(), p1.getBodyAngle(), p1.getTurretAngle(),
                p2.getX(), p2.getY(), p2.getBodyAngle(), p2.getTurretAngle());
    }

    /** Sends authoritative snapshots to both players of every active room. */
    public static void broadcastStates(DatagramChannel channel) {
        for (GameRoom room : Server.gameEngine.getRooms()) {
            String state = buildState(room);
            if (state == null) {
                continue;
            }
            sendUdpState(channel, state, room.getPlayer1Id());
            sendUdpState(channel, state, room.getPlayer2Id());
        }
    }

    private static void sendUdpState(DatagramChannel channel, String state, String playerId) {
        SocketAddress address = udpClients.get(playerId);
        if (address == null) {
            Player player = Server.players.get(playerId);
            address = player == null ? null : player.getUdpAddress();
        }
        if (address == null) {
            return;
        }

        try {
            channel.send(ByteBuffer.wrap(state.getBytes(StandardCharsets.UTF_8)), address);
        } catch (Exception e) {
            System.err.println("[Server] Could not send STATE to " + playerId + ": " + e.getMessage());
        }
    }

    /** Legacy shoot relay; authoritative bullet simulation is the next migration step. */
    private static void handleShoot(String[] parts, DatagramChannel channel, SocketAddress sender) throws Exception {
        // SHOOT|roomId|playerId|x|y|turretAngle
        if (parts.length != 6) {
            return;
        }

        String roomId = parts[1];
        String playerId = parts[2];
        double x = Double.parseDouble(parts[3]);
        double y = Double.parseDouble(parts[4]);
        double angle = Double.parseDouble(parts[5]);
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(angle)) {
            return;
        }

        GameRoom room = Server.gameEngine.getRoom(roomId);
        if (room == null || (!playerId.equals(room.getPlayer1Id())
                && !playerId.equals(room.getPlayer2Id()))) {
            return;
        }

        udpClients.put(playerId, sender);
        String opponentId = playerId.equals(room.getPlayer1Id())
                ? room.getPlayer2Id()
                : room.getPlayer1Id();
        SocketAddress target = udpClients.get(opponentId);
        if (target == null) {
            Player opponent = Server.players.get(opponentId);
            target = opponent == null ? null : opponent.getUdpAddress();
        }
        if (target == null) {
            return;
        }

        String response = String.format("SHOOT|%s|%.2f|%.2f|%.2f", playerId, x, y, angle);
        channel.send(ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8)), target);
    }

    private static void login(String[] parts, Channel clientChannel) {
        if (parts.length != 3 || !(clientChannel instanceof SocketChannel)) {
            return;
        }

        try (Connection connection = DataBaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM player WHERE name = ? AND password = ?")) {
            statement.setString(1, parts[1]);
            statement.setString(2, parts[2]);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    sendTcpResponse("LOGIN_FAILED", clientChannel);
                    return;
                }

                String playerId = resultSet.getString("id");
                String name = resultSet.getString("name");
                Server.players.put(playerId, new Player((SocketChannel) clientChannel, playerId, name));
                sendTcpResponse("LOGIN_SUCCESS|" + playerId, clientChannel);
            }
        } catch (SQLException e) {
            System.err.println("[Server] Login query failed: " + e.getMessage());
        }
    }

    private static void sendTcpResponse(String response, Channel clientChannel) {
        if (!(clientChannel instanceof SocketChannel)) {
            return;
        }

        try {
            ((SocketChannel) clientChannel).write(
                    ByteBuffer.wrap((response + "\n").getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            System.err.println("[Server] Could not send TCP response: " + e.getMessage());
        }
    }
}
