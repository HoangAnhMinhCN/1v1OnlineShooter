package com.mycompany.client.game;

/**
 * Lưu trữ một "bản ghi input" của người chơi tại một thời điểm cụ thể.
 *
 * <p>Mỗi khi client gửi INPUT lên server, một InputRecord tương ứng sẽ được
 * tạo và lưu vào {@link InputBuffer}. Khi server xác nhận (ACK) input đó,
 * các record cũ hơn hoặc bằng số thứ tự đã ACK sẽ bị xóa khỏi buffer.</p>
 *
 * <p>Record này bất biến (immutable) vì dữ liệu input không thay đổi sau khi
 * được tạo.</p>
 */
public final class InputRecord {

    /**
     * Số thứ tự (sequence number) của input này.
     * Tăng đơn điệu, dùng để khớp với ACK từ server.
     */
    public final long seq;

    /**
     * Bitmask các phím đang giữ tại thời điểm gửi input.
     * Bit 0 = W (lên), Bit 1 = S (xuống), Bit 2 = A (trái), Bit 3 = D (phải).
     */
    public final int keys;

    /**
     * Góc tháp pháo (độ) tại thời điểm gửi input.
     */
    public final double turretAngle;

    public InputRecord(long seq, int keys, double turretAngle) {
        this.seq = seq;
        this.keys = keys;
        this.turretAngle = turretAngle;
    }
}
