package com.mycompany.client.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Buffer vòng tròn lưu trữ lịch sử các INPUT chưa được server ACK.
 *
 * <p><b>Tại sao cần buffer này?</b></p>
 * <p>Trong kiến trúc client-side prediction, client di chuyển tank ngay lập tức
 * mà không chờ server xác nhận (để cảm giác phản hồi tức thì). Tuy nhiên,
 * server là nguồn sự thật (authoritative source). Khi server gửi STATE về,
 * client phải:</p>
 * <ol>
 *   <li>Đặt tank về đúng vị trí server (vị trí chính xác nhất).</li>
 *   <li>Replay lại tất cả input có seq > ackedSeq (các input server chưa xử lý
 *       hoặc chưa phản ánh trong STATE này).</li>
 * </ol>
 *
 * <p><b>Thread Safety:</b> Class này được truy cập hoàn toàn trên JavaFX
 * Application Thread (AnimationTimer + Platform.runLater), nên không cần
 * synchronization.</p>
 */
public final class InputBuffer {

    /** Số lượng input tối đa giữ trong buffer (~2 giây ở 60 input/giây). */
    private static final int MAX_SIZE = 128;

    /**
     * Deque hoạt động như hàng đợi FIFO:
     * - addLast()  → thêm input mới nhất vào đuôi
     * - pollFirst() → xóa input cũ nhất ở đầu
     */
    private static final Deque<InputRecord> buffer = new ArrayDeque<>(MAX_SIZE);

    private InputBuffer() { /* utility class — không khởi tạo */ }

    /**
     * Ghi một input mới vào buffer ngay khi nó được gửi lên server.
     * Nếu buffer đầy, input cũ nhất sẽ bị xóa (không nên xảy ra trong thực tế).
     *
     * @param record InputRecord chứa seq, keys, turretAngle
     */
    public static void record(InputRecord record) {
        if (buffer.size() >= MAX_SIZE) {
            // Buffer quá đầy → server có vấn đề không gửi ACK; xóa input cũ nhất.
            buffer.pollFirst();
        }
        buffer.addLast(record);
    }

    /**
     * Lấy danh sách tất cả input có seq > ackedSeq (input chưa được server ACK).
     * Danh sách trả về theo thứ tự thời gian (cũ → mới) để replay đúng thứ tự.
     *
     * @param ackedSeq Số thứ tự ACK mới nhất nhận từ server
     * @return Danh sách InputRecord chưa ACK, sắp xếp từ cũ đến mới
     */
    public static List<InputRecord> getUnacknowledged(long ackedSeq) {
        List<InputRecord> result = new ArrayList<>();
        for (InputRecord r : buffer) {
            if (r.seq > ackedSeq) {
                result.add(r);
            }
        }
        return result; // thứ tự từ cũ → mới (do Deque duyệt từ head)
    }

    /**
     * Xóa tất cả input có seq <= ackedSeq khỏi buffer.
     * Server đã xử lý chúng → không cần replay nữa.
     *
     * @param ackedSeq Số thứ tự ACK mới nhất nhận từ server
     */
    public static void discardAcknowledged(long ackedSeq) {
        // Xóa từ đầu deque vì các record được thêm theo thứ tự seq tăng dần.
        while (!buffer.isEmpty() && buffer.peekFirst().seq <= ackedSeq) {
            buffer.pollFirst();
        }
    }

    /** Xóa toàn bộ buffer (dùng khi game kết thúc hoặc reconnect). */
    public static void clear() {
        buffer.clear();
    }

    /** Trả về số lượng input chưa ACK hiện có trong buffer (dùng để debug). */
    public static int size() {
        return buffer.size();
    }
}
