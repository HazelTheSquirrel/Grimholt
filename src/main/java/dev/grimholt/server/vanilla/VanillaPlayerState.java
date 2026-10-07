package dev.grimholt.server.vanilla;

import java.util.UUID;

public final class VanillaPlayerState {
    public enum GameMode { SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR }

    private final UUID uuid;
    private String name = "";
    private boolean connected;
    private final VanillaInventory inventory = new VanillaInventory(41);
    private GameMode gameMode = GameMode.SURVIVAL;
    private float health = 20.0f;
    private float hunger = 20.0f;
    private float saturation = 5.0f;
    private int experience;
    private int level;
    private boolean dead;
    private double x, y, z;
    private float yaw, pitch;
    private boolean onGround;

    public VanillaPlayerState(UUID uuid) {
        this.uuid = java.util.Objects.requireNonNull(uuid, "uuid");
    }

    public UUID uuid() { return uuid; }
    public String name() { return name; }
    public boolean connected() { return connected; }
    public void connect(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        this.name = name;
        this.connected = true;
    }
    public void disconnect() { this.connected = false; }
    public VanillaInventory inventory() { return inventory; }
    public GameMode gameMode() { return gameMode; }
    public void gameMode(GameMode mode) { gameMode = java.util.Objects.requireNonNull(mode); }
    public float health() { return health; }
    public float hunger() { return hunger; }
    public float saturation() { return saturation; }
    public int experience() { return experience; }
    public int level() { return level; }
    public boolean dead() { return dead; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
    public boolean onGround() { return onGround; }

    public void position(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalArgumentException("position must be finite");
        }
        this.x = x; this.y = y; this.z = z; this.yaw = yaw; this.pitch = pitch;
        this.onGround = onGround;
    }

    public void damage(float amount) {
        if (amount < 0) throw new IllegalArgumentException("amount must be non-negative");
        if (gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR) return;
        health = Math.max(0, health - amount);
        dead = health == 0;
    }

    public void heal(float amount) {
        if (amount < 0) throw new IllegalArgumentException("amount must be non-negative");
        health = Math.min(20.0f, health + amount);
    }

    public void hunger(float amount) {
        hunger = Math.max(0, Math.min(20.0f, hunger + amount));
    }

    public void saturation(float amount) {
        saturation = Math.max(0, Math.min(hunger, saturation + amount));
    }

    public void experience(int value, int level) {
        if (value < 0 || level < 0) throw new IllegalArgumentException("experience and level must be non-negative");
        experience = value;
        this.level = level;
    }

    public void respawn() {
        health = 20.0f;
        hunger = 20.0f;
        saturation = 5.0f;
        dead = false;
    }
}
