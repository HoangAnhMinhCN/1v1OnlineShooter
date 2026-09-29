package com.mycompany.client;

import com.mycompany.client.game.InputBuffer;
import com.mycompany.client.game.InputRecord;
import com.mycompany.client.game.Tank;

/**
 * Tạo và gửi các packet gameplay qua UDP.
 *
 * <p>Mỗi lần gửi INPUT, một {@link InputRecord} được lưu vào {@link InputBuffer}.
 * Đây là bước đầu tiên của cơ chế client-side prediction reconciliation.</p>
 */
public final class GamePacketSender {
    private GamePacketSender() {
    }

    /**
     * Số thứ tự input tiếp theo sẽ được gửi.
     * Tăng đơn điệu, không bao giờ reset (trừ khi khởi động lại app).
     */
    private static long nextInputSeq;

    /**
     * Gửi một INPUT packet lên server và lưu vào buffer để reconciliation.
     *
     * <p><b>Flow:</b></p>
     * <ol>
     *   <li>Đọc trạng thái phím và góc tháp pháo từ tank.</li>
     *   <li>Lưu vào {@link InputBuffer} với seq hiện tại.</li>
     *   <li>Gửi packet UDP lên server.</li>
     * </ol>
     *
     * <p>Thứ tự bước 2 trước bước 3 đảm bảo record luôn có trong buffer
     * trước khi server có cơ hội ACK (tránh race condition lý thuyết).</p>
     */
    public static void sendInput(Client client, Tank tank) {
        if (client == null || tank == null)
            return;

        long seq = nextInputSeq++;
        int keys = tank.getInputMask();
        double turretAngle = tank.getTurretAngle();

        // ── Bước 1: Lưu input vào buffer TRƯỚC khi gửi ──────────────────────
        // StatePacketHandler sẽ dùng buffer này để replay khi reconcile.
        InputBuffer.record(new InputRecord(seq, keys, turretAngle));

        // ── Bước 2: Gửi packet UDP lên server ──────────────────────────────
        String packet = String.format(
                "INPUT|%s|%s|%d|%d|%.2f",
                client.getGameRoomId(),
                client.getPlayerId(),
                seq,
                keys,
                turretAngle);

        client.sendUdpData(packet);
    }

    public static void sendShoot(Client client, Tank tank) {
        if (client == null || tank == null) {
            return;
        }

        String packet = String.format(
                "SHOOT|%s|%s|%.2f|%.2f|%.2f",
                client.getGameRoomId(),
                tank.getIdPlayer(),
                tank.getCenterX(),
                tank.getCenterY(),
                tank.getTurretAngle());

        client.sendUdpData(packet);
    }
}
