package com.mycompany.client.game;

import java.util.List;

/**
 * Đại diện cho xe tăng của người chơi trong game.
 * Di chuyển top-down 4 hướng: W=lên, S=xuống, A=trái, D=phải.
 * Thân xe tự xoay mặt theo hướng đang đi.
 */
public class Tank {
    private final String idPlayer;

    // ── Hằng số ──────────────────────────────────────────────────────────────
    public static final int WIDTH = 36; // px
    public static final int HEIGHT = 36; // px
    public static final double SPEED = 1; // px/frame (~150 px/s ở 60 FPS)
    /** Độ mờ khi ở trong bụi rậm (0.0 = vô hình, 1.0 = rõ hoàn toàn) */
    public static final double BUSH_OPACITY = 0.30;
    /** Tốc độ chuyển đổi opacity mỗi frame (lerp factor) */
    private static final double OPACITY_LERP = 0.08;

    // ── Trạng thái ───────────────────────────────────────────────────────────
    private double x, y; // vị trí góc trên-trái (pixel)
    // Vị trí server mới gửi về, dùng làm đích nội suy cho tank đối thủ.
    private double targetX, targetY;
    // Chỉ tank đối thủ mới dùng nội suy từ vị trí server gửi về.
    private boolean remoteControlled;
    private List<Tank> otherTanks;
    private double angleTank; // góc quay thân xe (độ, 0 = lên trên, 90 = sang phải)
    private double turretAngle;
    private boolean inBush; // xe đang ở tile bụi rậm (tile type 0)
    private double opacity = 1.0; // opacity hiện tại (smooth lerp)

    // ── Màu sắc ───────────────────────────────────────────────────────────────
    private final String bodyColor;
    private final String turretColor;

    // ── Trạng thái điều khiển (top-down 4 hướng) ─────────────────────────────
    private boolean moveUp, moveDown, moveLeft, moveRight;

    public Tank(double startX, double startY, String bodyColor, String turretColor, String idPlayer) {
        this.x = startX;
        this.y = startY;
        this.targetX = startX;
        this.targetY = startY;
        this.angleTank = 0; // mặt lên trên
        this.turretAngle = 0;
        this.bodyColor = bodyColor;
        this.turretColor = turretColor;
        this.idPlayer = idPlayer;
    }

    // ── Cập nhật trạng thái mỗi frame ────────────────────────────────────────

    /**
     * Cập nhật vị trí và hướng xe tăng mỗi frame (gọi bởi AnimationTimer).
     *
     * <p>Với tank đối thủ (remoteControlled = true): nội suy mượt về targetX/targetY
     * là vị trí server gửi về, tránh teleport giật.</p>
     *
     * <p>Với tank cục bộ (remoteControlled = false): áp dụng input hiện tại của
     * người chơi (prediction). Vị trí này sẽ bị reconcile khi server gửi STATE.</p>
     */
    public void update() {
        if (remoteControlled) {
            // ── Tank đối thủ ──────────────────────────────────────────────────
            // Lerp (Linear Interpolation) mượt về vị trí server gửi về.
            // Hệ số 0.20 = mỗi frame tiến 20% khoảng cách còn lại → cảm giác
            // "trượt" tự nhiên, không teleport đột ngột.
            double nextX = x + (targetX - x) * 0.20;
            double nextY = y + (targetY - y) * 0.20;
            if (canMoveTo(nextX, y))
                x = nextX;
            if (canMoveTo(x, nextY))
                y = nextY;
        } else {
            // ── Tank cục bộ: Client-Side Prediction ───────────────────────────
            // Áp dụng input hiện tại ngay lập tức, không chờ server.
            // Kết quả này có thể bị "sửa" bởi reconciliation khi STATE về.
            applyMovement(moveUp, moveDown, moveLeft, moveRight);
        }

        // Cập nhật hiệu ứng bụi rậm (visual only, không ảnh hưởng vị trí)
        updateBushState();
        double targetOpacity = inBush ? BUSH_OPACITY : 1.0;
        opacity += (targetOpacity - opacity) * OPACITY_LERP;
    }

    /**
     * Replay một input frame trong quá trình reconciliation.
     *
     * <p><b>Reconciliation flow:</b></p>
     * <ol>
     *   <li>StatePacketHandler nhận STATE từ server với vị trí authoritative.</li>
     *   <li>Tank được đặt về vị trí đó (setPosition).</li>
     *   <li>Với mỗi input chưa ACK (seq > ackedSeq), phương thức này được gọi
     *       để tính lại vị trí, giúp bù lại độ trễ mạng.</li>
     * </ol>
     *
     * <p><b>Tại sao không dùng update() trực tiếp?</b><br>
     * update() có thêm logic lerp cho remote tank, bush state, opacity... không
     * cần thiết trong replay. simulateInputStep() chỉ tính vật lý thuần túy,
     * khớp với cách ServerTank.simulateTick() hoạt động.</p>
     *
     * <p><b>Không kiểm tra va chạm tank-tank</b> khi replay để khớp với server
     * (ServerTank.canMoveTo() cũng không kiểm tra va chạm với tank khác).</p>
     *
     * @param keys bitmask phím: bit0=W, bit1=S, bit2=A, bit3=D
     */
    public void simulateInputStep(int keys) {
        boolean up    = (keys & 1) != 0;
        boolean down  = (keys & 2) != 0;
        boolean left  = (keys & 4) != 0;
        boolean right = (keys & 8) != 0;
        // Replay không update bush/opacity để tránh flicker visual
        applyMovement(up, down, left, right);
    }

    /**
     * Logic di chuyển dùng chung cho cả update() và simulateInputStep().
     * Tách ra để tránh lặp code và đảm bảo client/server tính toán giống nhau.
     *
     * @param up    có đang nhấn W/↑ không
     * @param down  có đang nhấn S/↓ không
     * @param left  có đang nhấn A/← không
     * @param right có đang nhấn D/→ không
     */
    private void applyMovement(boolean up, boolean down, boolean left, boolean right) {
        double dx = 0, dy = 0;

        if (up)    dy -= SPEED;
        if (down)  dy += SPEED;
        if (left)  dx -= SPEED;
        if (right) dx += SPEED;

        // Chuẩn hóa vector chéo: di chuyển chéo không nhanh hơn di chuyển thẳng.
        // Không chuẩn hóa → tốc độ chéo = SPEED * √2 ≈ 1.41x tốc độ thẳng.
        if (dx != 0 && dy != 0) {
            dx /= Math.sqrt(2);
            dy /= Math.sqrt(2);
        }

        // Xoay thân xe theo hướng vector di chuyển.
        // atan2(dx, -dy): góc 0° = hướng lên (trục Y âm trong canvas).
        if (dx != 0 || dy != 0) {
            angleTank = Math.toDegrees(Math.atan2(dx, -dy));
        }

        // Sliding collision: thử X và Y độc lập để xe "trượt" dọc tường.
        // Nếu kiểm tra cùng lúc cả X+Y thì va tường chéo sẽ bị kẹt hoàn toàn.
        if (dx != 0 && canMoveTo(x + dx, y))
            x += dx;
        if (dy != 0 && canMoveTo(x, y + dy))
            y += dy;
    }

    // private boolean hasMovementInput() {
    // return moveUp || moveDown || moveLeft || moveRight;
    // }

    /**
     * Cập nhật trạng thái inBush dựa trên tile tâm xe tăng.
     */
    private void updateBushState() {
        int col = (int) (getCenterX() / GameMap.TILE_SIZE);
        int row = (int) (getCenterY() / GameMap.TILE_SIZE);
        if (row < 0 || row >= GameMap.ROWS || col < 0 || col >= GameMap.COLS) {
            inBush = false;
            return;
        }
        inBush = (GameMap.MAP_DATA[row][col] == 0); // 0 = cỏ / bụi rậm
    }

    /**
     * Kiểm tra xe tăng có thể di chuyển tới vị trí (nx, ny) hay không.
     */
    private boolean canMoveTo(double nx, double ny) {
        int m = 3; // margin pixel
        if (!(isTilePassable(nx + m, ny + m)
                && isTilePassable(nx + WIDTH - m, ny + m)
                && isTilePassable(nx + m, ny + HEIGHT - m)
                && isTilePassable(nx + WIDTH - m, ny + HEIGHT - m)))
            return false;

        if (otherTanks != null) {
            for (Tank other : otherTanks) {
                if (other != null && other != this && rectanglesOverlap(nx, ny, other.x, other.y))
                    return false;
            }
        }
        return true;
    }

    private boolean rectanglesOverlap(double ax, double ay, double bx, double by) {
        double gap = 2.0;
        return ax < bx + WIDTH + gap && ax + WIDTH + gap > bx
                && ay < by + HEIGHT + gap && ay + HEIGHT + gap > by;
    }

    /**
     * Tile tại pixel (px, py) có thể đi qua không?
     */
    private boolean isTilePassable(double px, double py) {
        int col = (int) (px / GameMap.TILE_SIZE);
        int row = (int) (py / GameMap.TILE_SIZE);
        if (row < 0 || row >= GameMap.ROWS || col < 0 || col >= GameMap.COLS)
            return false;
        return GameMap.MAP_DATA[row][col] != 1; // 1 = tường
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getAngle() {
        return angleTank;
    }

    /** Góc tháp pháo = 0 (tháp luôn cùng hướng thân xe) */
    public double getTurretAngle() {
        return turretAngle;
    }

    public String getBodyColor() {
        return bodyColor;
    }

    public String getTurretColor() {
        return turretColor;
    }

    public double getCenterX() {
        return x + WIDTH / 2.0;
    }

    public double getCenterY() {
        return y + HEIGHT / 2.0;
    }

    /** Opacity hiện tại (đã lerp mượt mà). Dùng cho GameRender. */
    public double getOpacity() {
        return opacity;
    }

    /** Xe tăng đang trong bụi rậm không? */
    public boolean isInBush() {
        return inBush;
    }

    // ── Setters điều khiển ────────────────────────────────────────────────────

    public void setMoveUp(boolean v) {
        moveUp = v;
    }

    public void setMoveDown(boolean v) {
        moveDown = v;
    }

    public void setMoveLeft(boolean v) {
        moveLeft = v;
    }

    public void setMoveRight(boolean v) {
        moveRight = v;
    }

    // set goc phao
    public void setTurretAngle(double turretAngle) {
        this.turretAngle = turretAngle;
    }

    // Cap nhat vij tri tank doi thu nhan tu server
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;
    }

    public void setTargetPosition(double x, double y) {
        // Không dịch chuyển ngay lập tức để tránh hiện tượng giật hình.
        this.targetX = x;
        this.targetY = y;
    }

    public void setRemoteControlled(boolean remoteControlled) {
        this.remoteControlled = remoteControlled;
    }

    public void setOtherTanks(List<Tank> otherTanks) {
        this.otherTanks = otherTanks;
    }

    // set goc than xe cho tank doi thu (goc thap phao dung setTurretAngle)
    public void setAngle(double angle) {
        this.angleTank = angle;
    }

    public String getIdPlayer() {
        return idPlayer;
    }

    public int getInputMask() {
        int keys = 0;
        if (moveUp)
            keys |= 1;
        if (moveDown)
            keys |= 2;
        if (moveLeft)
            keys |= 4;
        if (moveRight)
            keys |= 8;
        return keys;
    }
}
