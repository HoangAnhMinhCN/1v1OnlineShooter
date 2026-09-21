package com.mycompany.server.game;

public final class ServerTank {
    public static final double WIDTH = 36; // px
    public static final double HEIGHT = 36; // px
    public static final double SPEED_PER_TICK = 1.0; // px/frame (~150 px/s ở 60 FPS)

    private final String playerId;
    private double x;
    private double y;
    private double bodyAngle; // góc quay thân xe (độ, 0 = lên trên, 90 = sang phải)
    private double turretAngle;

    private int keys;
    private long lastInputSeq = -1;

    public ServerTank(String playerId, double startX, double startY) {
        this.playerId = playerId;
        this.x = startX;
        this.y = startY;
    }

    public void applyInput(long seq, int keys, double turretAngle) {
        if (seq <= lastInputSeq) {
            return; // packet cu hoac bi trung
        }
        this.lastInputSeq = seq;
        this.keys = keys;
        this.turretAngle = turretAngle;
    }

    public void simulate() {
        double dx = 0;
        double dy = 0;

        if ((keys & 1) != 0)
            dy -= SPEED_PER_TICK; // moveUp
        if ((keys & 2) != 0)
            dy += SPEED_PER_TICK; // moveDown
        if ((keys & 4) != 0)
            dx -= SPEED_PER_TICK; // left
        if ((keys & 8) != 0)
            dx += SPEED_PER_TICK; // right

        // xu ly di cheo de co cung toc do
        if (dx != 0 && dy != 0) {
            dx /= Math.sqrt(2);
            dy /= Math.sqrt(2);
        }

        // tinh goc than xe
        if (dx != 0 || dy != 0) {
            bodyAngle = Math.toDegrees(Math.atan2(dx, -dy));
        }
        // kiem tra gioi han map
        if (canMoveTo(x + dx, y))
            x += dx;
        if (canMoveTo(x, y + dy))
            y += dy;
    }

    // next x nexy la vi tri goc ben phai cua xe tanh
    private boolean canMoveTo(double nextX, double nextY) {
        int margin = 3;
        return isPassable(nextX + margin, nextY + margin)
                && isPassable(nextX + WIDTH - margin, nextY + margin)
                && isPassable(nextX + margin, nextY + HEIGHT - margin)
                && isPassable(nextX + WIDTH - margin, nextY + HEIGHT - margin);
    }

    private boolean isPassable(double px, double py) {
        int col = (int) (px / GameMap.TILE_SIZE);
        int row = (int) (py / GameMap.TILE_SIZE);

        if (row < 0 || row >= GameMap.ROWS || col < 0 || col >= GameMap.COLS) {
            return false;
        }

        return GameMap.MAP_DATA[row][col] != 1;
    }

    public String getPlayerId() {
        return playerId;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getBodyAngle() {
        return bodyAngle;
    }

    public double getTurretAngle() {
        return turretAngle;
    }

    public long getLastInputSeq() {
        return lastInputSeq;
    }
}
