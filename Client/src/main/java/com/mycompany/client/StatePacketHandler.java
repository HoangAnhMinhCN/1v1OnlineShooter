package com.mycompany.client;

import java.util.ArrayList;
import java.util.List;

import com.mycompany.client.game.GameScene;
import com.mycompany.client.game.InputBuffer;
import com.mycompany.client.game.InputRecord;
import com.mycompany.client.game.Tank;
import javafx.application.Platform;

/**
 * Xử lý gói tin STATE từ server và thực hiện Client-Side Prediction Reconciliation.
 *
 * <h2>Vấn đề cần giải quyết</h2>
 * <p>Trong game multiplayer, có 2 lựa chọn cực đoan:</p>
 * <ul>
 *   <li><b>Chờ server</b>: Tank chỉ di chuyển sau khi nhận STATE từ server.
 *       → Cực kỳ lag vì phải chờ round-trip (client gửi → server xử lý → client nhận).</li>
 *   <li><b>Di chuyển cục bộ hoàn toàn</b>: Bỏ qua server, client tự quyết.
 *       → Không đồng bộ giữa các người chơi, dễ cheat.</li>
 * </ul>
 *
 * <h2>Giải pháp: Client-Side Prediction + Reconciliation</h2>
 * <p>Kết hợp cả hai: client di chuyển ngay (prediction), nhưng khi STATE về
 * thì "hiệu chỉnh" lại (reconciliation) để đồng bộ với server.</p>
 *
 * <h2>Flow hoàn chỉnh mỗi STATE nhận được</h2>
 * <pre>
 * Frame N:   Client nhấn W → Tank.update() di chuyển ngay (prediction)
 *            GamePacketSender.sendInput(seq=5, keys=W) → server
 *            InputBuffer lưu: {seq=5, keys=W}
 *
 * Frame N+3: STATE về: tick=100, ack1=3 (server đã xử lý đến seq=3)
 *            Reconciliation:
 *              1. Đặt tank về vị trí server (authoritative x, y)
 *              2. InputBuffer.getUnacknowledged(3) → [{seq=4,W}, {seq=5,W}]
 *              3. Replay từng input: simulateInputStep(W) x2
 *              4. Tank giờ ở vị trí = server_pos + 2 frames di chuyển
 *            → Tank không bị giật về vị trí cũ!
 * </pre>
 */
public final class StatePacketHandler {

    /**
     * Tick server cao nhất đã nhận. Dùng để lọc gói tin đến muộn
     * (out-of-order UDP packets — UDP không đảm bảo thứ tự).
     */
    private static long lastReceivedServerTick = -1;
    private static long lastAcknowledgedInputSeq = -1;

    private StatePacketHandler() {
    }

    /**
     * Xử lý một gói STATE nhận từ server.
     *
     * <p>Format: {@code STATE|roomId|tick|ack1|ack2|x1|y1|body1|turret1|x2|y2|body2|turret2}</p>
     * <ul>
     *   <li>{@code ack1/ack2} = seq input cuối cùng của mỗi player mà server đã xử lý.</li>
     *   <li>{@code x1,y1,body1,turret1} = trạng thái authoritative của player 1.</li>
     * </ul>
     *
     * @param parts mảng string đã split theo '|'
     */
    public static void handle(String[] parts) {
        // STATE|roomId|tick|ack1|ack2|x1|y1|body1|turret1|hp1|x2|y2|body2|turret2|hp2|destroyedCount|ids...
        if (parts.length < 15) {
            System.err.println("[StatePacketHandler] Invalid STATE packet");
            return;
        }

        try {
            Client client = Client.getInstance();
            // Bỏ qua STATE không thuộc về room này
            if (client == null || !parts[1].equals(client.getGameRoomId())) {
                return;
            }

            long serverTick = Long.parseLong(parts[2]);
            long ack1 = Long.parseLong(parts[3]);
            long ack2 = Long.parseLong(parts[4]);

            double x1 = Double.parseDouble(parts[5]);
            double y1 = Double.parseDouble(parts[6]);
            double body1 = Double.parseDouble(parts[7]);
            double turret1 = Double.parseDouble(parts[8]);
            int hp1 = Integer.parseInt(parts[9]);
            double x2 = Double.parseDouble(parts[10]);
            double y2 = Double.parseDouble(parts[11]);
            double body2 = Double.parseDouble(parts[12]);
            double turret2 = Double.parseDouble(parts[13]);
            int hp2 = Integer.parseInt(parts[14]);

            if (serverTick < 0 || !Double.isFinite(x1) || !Double.isFinite(y1)
                    || !Double.isFinite(body1) || !Double.isFinite(turret1)
                    || !Double.isFinite(x2) || !Double.isFinite(y2)
                    || !Double.isFinite(body2) || !Double.isFinite(turret2)) {
                return;
            }

            List<String> destroyedBulletIds = new ArrayList<>();
            if (parts.length > 15) {
                int destroyedCount = Integer.parseInt(parts[15]);
                if (destroyedCount >= 0 && parts.length >= 16 + destroyedCount) {
                    for (int i = 0; i < destroyedCount; i++) {
                        destroyedBulletIds.add(parts[16 + i]);
                    }
                }
            }

            if (serverTick < 0
                    || !Double.isFinite(x1) || !Double.isFinite(y1)
                    || !Double.isFinite(body1) || !Double.isFinite(turret1)
                    || !Double.isFinite(x2) || !Double.isFinite(y2)
                    || !Double.isFinite(body2) || !Double.isFinite(turret2)) {
                return;
            }

            boolean amPlayer1 = client.getMyNumber() == 1;
            long myAck = amPlayer1 ? ack1 : ack2;

            synchronized (StatePacketHandler.class) {
                if (serverTick <= lastReceivedServerTick) {
                    return;
                }
                lastReceivedServerTick = serverTick;
                lastAcknowledgedInputSeq = myAck;
            }

            Platform.runLater(() -> applyStateWithReconciliation(
                    client, amPlayer1, myAck,
                    x1, y1, body1, turret1, hp1,
                    x2, y2, body2, turret2, hp2,
                    destroyedBulletIds));

        } catch (NumberFormatException e) {
            System.err.println("[StatePacketHandler] Invalid STATE values: " + e.getMessage());
        }
    }

    public static void handleDestroyedBullet(String[] parts) {
        if (parts.length != 3) {
            return;
        }

        Client client = Client.getInstance();
        if (client == null || !parts[1].equals(client.getGameRoomId())) {
            return;
        }

        String bulletId = parts[2];
        Platform.runLater(() -> {
            GameScene scene = GameScene.getInstance();
            if (scene != null) {
                scene.removeDestroyedBullets(List.of(bulletId));
            }
        });
    }

    /**
     * Áp dụng STATE từ server và thực hiện reconciliation cho tank cục bộ.
     *
     * <p>Chạy trên JavaFX Application Thread (an toàn với AnimationTimer).</p>
     *
     * @param amPlayer1     true nếu người chơi này là player 1
     * @param myAck         seq ACK của mình từ server
     * @param pendingInputs danh sách input chưa ACK cần replay
     */
    private static void applyStateWithReconciliation(
            Client client, boolean amPlayer1, long myAck,
            double x1, double y1, double body1, double turret1, int hp1,
            double x2, double y2, double body2, double turret2, int hp2,
            List<String> destroyedBulletIds) {

        GameScene scene = GameScene.getInstance();
        if (scene == null) {
            return;
        }

        if (!destroyedBulletIds.isEmpty()) {
            scene.removeDestroyedBullets(destroyedBulletIds);
        }

        Tank localTank = scene.getLocalTank();
        Tank enemyTank = scene.getTank(client.getAnotherPlayerId());
        if (localTank == null || enemyTank == null) {
            return;
        }

        // ── Phân biệt dữ liệu của mình và đối thủ ──────────────────────────
        double myX, myY, myBody, myTurret;
        double enemyX, enemyY, enemyBody, enemyTurret;
        int myHp, enemyHp;
        if (amPlayer1) {
            myX = x1; myY = y1; myBody = body1; myTurret = turret1; myHp = hp1;
            enemyX = x2; enemyY = y2; enemyBody = body2; enemyTurret = turret2; enemyHp = hp2;
        } else {
            myX = x2; myY = y2; myBody = body2; myTurret = turret2; myHp = hp2;
            enemyX = x1; enemyY = y1; enemyBody = body1; enemyTurret = turret1; enemyHp = hp1;
        }
        scene.updateHpBars(hp1, hp2);

        // ── Bước 1: Đặt tank về vị trí authoritative từ server ──────────────
        // Đây là "điểm bắt đầu" để replay. Server luôn đúng.
        localTank.setPosition(myX, myY);
        localTank.setAngle(myBody);
        localTank.setTurretAngle(myTurret);
        localTank.setHp(myHp);

        // ── Bước 2: Replay các input chưa ACK (Reconciliation) ───────────────
        // Server gửi STATE phản ánh trạng thái SAU KHI xử lý input đến seq=myAck.
        // Các input có seq > myAck chưa được server tích hợp → client tự replay
        // để tính vị trí "dự đoán hiện tại" bù vào độ trễ mạng.
        //
        // Kết quả: tank không bị giật về vị trí cũ mỗi khi STATE về.
        List<InputRecord> pendingInputs = InputBuffer.getUnacknowledged(myAck);
        for (InputRecord input : pendingInputs) {
            localTank.simulateInputStep(input.keys);
        }

        // ── Bước 3: Dọn buffer ───────────────────────────────────────────────
        // Xóa input đã ACK → giữ buffer nhỏ, tránh replay dư.
        InputBuffer.discardAcknowledged(myAck);

        // ── Bước 4: Cập nhật tank đối thủ bằng lerp ─────────────────────────
        // Đối thủ KHÔNG có prediction → setTargetPosition() để Tank.update()
        // lerp mượt về đích, tránh teleport giật.
        enemyTank.setTargetPosition(enemyX, enemyY);
        enemyTank.setAngle(enemyBody);
        enemyTank.setTurretAngle(enemyTurret);
        enemyTank.setHp(enemyHp);

    }

    public static synchronized long getLastAcknowledgedInputSeq() {
        return lastAcknowledgedInputSeq;
    }
}

