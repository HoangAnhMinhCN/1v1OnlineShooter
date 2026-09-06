package com.mycompany.client.game;

public class Bullet {
    private String id; // VD: "P1_0", "P1_1"
    private double x;
    private double y;
    private double speedX;
    private double speedY;
    private double angle;
    private boolean active; // true: đang bay, false: biến mất
    private int idPlayer;

    private static final double SPEED = 8.0; // Tốc độ bay của đạn

    public Bullet(double x, double y, double angle, int idPlayer) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.idPlayer = idPlayer;
        this.active = true; // đặt true để đạn được render ngay khi sinh ra

        // Tính toán vận tốc theo góc bắn (-90 độ do nòng pháo hướng 12h)
        double rad = Math.toRadians(angle - 90);
        this.speedX = Math.cos(rad) * SPEED;
        this.speedY = Math.sin(rad) * SPEED;
    }

    // Constructor phục vụ việc tạo chuỗi ID nếu cần đồng bộ Server
    public Bullet(String id, double x, double y, double angle, int idPlayer) {
        this(x, y, angle, idPlayer);
        this.id = id;
    }

    public void update() {
        if (!active)
            return;

        // 1. Di chuyển đạn
        x += speedX;
        y += speedY;

        // 2. Kiểm tra va chạm rìa bản đồ (Out of Bounds)
        double mapWidth = GameMap.COLS * GameMap.TILE_SIZE;
        double mapHeight = GameMap.ROWS * GameMap.TILE_SIZE;

        if (x < 0 || x > mapWidth || y < 0 || y > mapHeight) {
            this.active = false; // Hủy đạn khi bay ra ngoài Map
            return;
        }

        // 3. Kiểm tra va chạm tường gạch (Wall Collision)
        int col = (int) (x / GameMap.TILE_SIZE);
        int row = (int) (y / GameMap.TILE_SIZE);

        if (row >= 0 && row < GameMap.ROWS && col >= 0 && col < GameMap.COLS) {
            if (GameMap.MAP_DATA[row][col] == 1) { // 1 là Tường gạch/Vật cản
                this.active = false; // Đâm vào tường thì biến mất
            }
        }
    }

    // Getters và Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getIdPlayer() {
        return idPlayer;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}