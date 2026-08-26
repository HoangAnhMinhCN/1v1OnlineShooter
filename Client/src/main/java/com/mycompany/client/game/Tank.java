package com.mycompany.client.game;

/**
 * Đại diện cho xe tăng của người chơi trong game.
 * Di chuyển top-down 4 hướng: W=lên, S=xuống, A=trái, D=phải.
 * Thân xe tự xoay mặt theo hướng đang đi.
 */
public class Tank {

    // ── Hằng số ──────────────────────────────────────────────────────────────
    public static final int WIDTH  = 36; // px
    public static final int HEIGHT = 36; // px
    public static final double SPEED = 2.5; // px/frame (~150 px/s ở 60 FPS)
    /** Độ mờ khi ở trong bụi rậm (0.0 = vô hình, 1.0 = rõ hoàn toàn) */
    public static final double BUSH_OPACITY = 0.30;
    /** Tốc độ chuyển đổi opacity mỗi frame (lerp factor) */
    private static final double OPACITY_LERP = 0.08;

    // ── Trạng thái ───────────────────────────────────────────────────────────
    private double x, y;      // vị trí góc trên-trái (pixel)
    private double angle;     // góc quay thân xe (độ, 0 = lên trên, 90 = sang phải)
    private boolean inBush;   // xe đang ở tile bụi rậm (tile type 0)
    private double opacity = 1.0; // opacity hiện tại (smooth lerp)

    // ── Màu sắc ───────────────────────────────────────────────────────────────
    private final String bodyColor;
    private final String turretColor;

    // ── Trạng thái điều khiển (top-down 4 hướng) ─────────────────────────────
    private boolean moveUp, moveDown, moveLeft, moveRight;

    public Tank(double startX, double startY, String bodyColor, String turretColor) {
        this.x = startX;
        this.y = startY;
        this.angle = 0; // mặt lên trên
        this.bodyColor = bodyColor;
        this.turretColor = turretColor;
    }

    // ── Cập nhật trạng thái mỗi frame ────────────────────────────────────────

    /**
     * Cập nhật vị trí và hướng xe tăng, kiểm tra collision với tile map.
     */
    public void update() {
        double dx = 0, dy = 0;

        if (moveUp)    dy -= SPEED;
        if (moveDown)  dy += SPEED;
        if (moveLeft)  dx -= SPEED;
        if (moveRight) dx += SPEED;

        // Di chuyển chéo: chuẩn hóa vector để tốc độ không tăng gấp đôi
        if (dx != 0 && dy != 0) {
            double norm = Math.sqrt(2);
            dx /= norm;
            dy /= norm;
        }

        // Xoay thân xe theo hướng đang đi
        if (dx != 0 || dy != 0) {
            // atan2(dx, -dy): 0° = lên, 90° = phải, -90° = trái, 180° = xuống
            angle = Math.toDegrees(Math.atan2(dx, -dy));
        }

        // Sliding collision: thử X riêng, rồi Y riêng
        if (dx != 0 && canMoveTo(x + dx, y)) x += dx;
        if (dy != 0 && canMoveTo(x, y + dy)) y += dy;

        // Kiểm tra tile tâm xe — bụi rậm = tile type 0
        updateBushState();

        // Smooth lerp opacity về mục tiêu
        double targetOpacity = inBush ? BUSH_OPACITY : 1.0;
        opacity += (targetOpacity - opacity) * OPACITY_LERP;
    }

    /**
     * Cập nhật trạng thái inBush dựa trên tile tâm xe tăng.
     */
    private void updateBushState() {
        int col = (int)(getCenterX() / GameMap.TILE_SIZE);
        int row = (int)(getCenterY() / GameMap.TILE_SIZE);
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
        return isTilePassable(nx + m,           ny + m)
            && isTilePassable(nx + WIDTH - m,   ny + m)
            && isTilePassable(nx + m,           ny + HEIGHT - m)
            && isTilePassable(nx + WIDTH - m,   ny + HEIGHT - m);
    }

    /**
     * Tile tại pixel (px, py) có thể đi qua không?
     */
    private boolean isTilePassable(double px, double py) {
        int col = (int)(px / GameMap.TILE_SIZE);
        int row = (int)(py / GameMap.TILE_SIZE);
        if (row < 0 || row >= GameMap.ROWS || col < 0 || col >= GameMap.COLS) return false;
        return GameMap.MAP_DATA[row][col] != 1; // 1 = tường
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public double getX()          { return x; }
    public double getY()          { return y; }
    public double getAngle()      { return angle; }
    /** Góc tháp pháo = 0 (tháp luôn cùng hướng thân xe) */
    public double getTurretAngle(){ return 0; }
    public String getBodyColor()  { return bodyColor; }
    public String getTurretColor(){ return turretColor; }

    public double getCenterX() { return x + WIDTH  / 2.0; }
    public double getCenterY() { return y + HEIGHT / 2.0; }

    /** Opacity hiện tại (đã lerp mượt mà). Dùng cho GameRender. */
    public double getOpacity()    { return opacity; }
    /** Xe tăng đang trong bụi rậm không? */
    public boolean isInBush()     { return inBush; }

    // ── Setters điều khiển ────────────────────────────────────────────────────

    public void setMoveUp   (boolean v) { moveUp    = v; }
    public void setMoveDown (boolean v) { moveDown  = v; }
    public void setMoveLeft (boolean v) { moveLeft  = v; }
    public void setMoveRight(boolean v) { moveRight = v; }
}
