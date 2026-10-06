package dev.grimholt.server.vanilla;

import java.util.UUID;

public final class VanillaPlayerState {
    public enum GameMode { SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR }

    private final UUID uuid;
    private final VanillaInventory inventory = new VanillaInventory(41);
    private GameMode gameMode = GameMode.SURVIVAL;
    private float health = 20.0f;
    private float hunger = 20.0f;
    private float saturation = 5.0f;
    private int experience;
    private int level;
    private boolean dead;

    public VanillaPlayerState(UUID uuid) {
        this.uuid = java.util.Objects.requireNonNull(uuid, "uuid");
    }

    public UUID uuid() { return uuid; }
    public VanillaInventory inventory() { return inventory; }
    public GameMode gameMode() { return gameMode; }
    public void gameMode(GameMode mode) { gameMode = java.util.Objects.requireNonNull(mode); }
    public float health() { return health; }
    public float hunger() { return hunger; }
    public float saturation() { return saturation; }
    public int experience() { return experience; }
    public int level() { return level; }
    public boolean dead() { return dead; }

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
