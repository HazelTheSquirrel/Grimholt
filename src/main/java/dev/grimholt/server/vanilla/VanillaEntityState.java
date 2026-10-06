package dev.grimholt.server.vanilla;

import java.util.Objects;
import java.util.UUID;

public final class VanillaEntityState {
    private final UUID uuid;
    private final String typeId;
    private double x;
    private double y;
    private double z;
    private double velocityX;
    private double velocityY;
    private double velocityZ;
    private float health;
    private boolean removed;

    public VanillaEntityState(UUID uuid, String typeId, double x, double y, double z, float health) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.typeId = Objects.requireNonNull(typeId, "typeId");
        if (health < 0) throw new IllegalArgumentException("health must be non-negative");
        this.x = x; this.y = y; this.z = z; this.health = health;
    }

    public UUID uuid() { return uuid; }
    public String typeId() { return typeId; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public float health() { return health; }
    public boolean removed() { return removed; }

    public void move(double dx, double dy, double dz) {
        ensureAlive();
        x += dx; y += dy; z += dz;
    }

    public void velocity(double x, double y, double z) {
        ensureAlive();
        velocityX = x; velocityY = y; velocityZ = z;
    }

    public double velocityX() { return velocityX; }
    public double velocityY() { return velocityY; }
    public double velocityZ() { return velocityZ; }

    public void damage(float amount) {
        ensureAlive();
        if (amount < 0) throw new IllegalArgumentException("amount must be non-negative");
        health = Math.max(0, health - amount);
        if (health == 0) removed = true;
    }

    public void remove() { removed = true; }

    private void ensureAlive() {
        if (removed) throw new IllegalStateException("entity is removed");
    }
}
