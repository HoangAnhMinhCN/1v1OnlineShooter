package com.mycompany.server.game;

public class ServerBullet {
    private final String id;
    private final String roomId;
    private final String ownerId; // PlayerId của người bắn
    private double x;
    private double y;
    private final double vx;
    private final double vy;
    private final double radius = 4.0; // Bán kính viên đạn (px)
    private boolean alive = true;

    public static final double BULLET_SPEED = 8.0; // Tốc độ đạn per tick
    public static final int BULLET_DAMAGE = 10; // Sát thương mỗi viên

    public ServerBullet(String id, String roomId, String ownerId, double x, double y, double angleDeg) {
        this.id = id;
        this.roomId = roomId;
        this.ownerId = ownerId;
        this.x = x;
        this.y = y;
        // Client dùng góc độ, 0 độ hướng lên trên.
        double angleRad = Math.toRadians(angleDeg - 90.0);
        this.vx = Math.cos(angleRad) * BULLET_SPEED;
        this.vy = Math.sin(angleRad) * BULLET_SPEED;
    }

    public void updatePosition() {
        this.x += vx;
        this.y += vy;
    }

    // Getters & Setters
    public String getId() {
        return id;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getRadius() {
        return radius;
    }

    public boolean isAlive() {
        return alive;
    }

    public void destroy() {
        this.alive = false;
    }
}
