package dev.grimholt.server.vanilla;

public final class FreezingState {
    public static final int MAX_TICKS = 140;
    private int ticks;
    private boolean active;

    public void apply(int durationTicks) {
        if (durationTicks < 0) throw new IllegalArgumentException("durationTicks must be non-negative");
        active = true;
        ticks = Math.max(ticks, durationTicks);
    }

    public void tick() {
        if (ticks > 0) ticks--;
        if (ticks == 0) active = false;
    }

    public boolean active() { return active; }
    public int remainingTicks() { return ticks; }
    public boolean shouldShake() { return active; }
    public boolean shouldDamage() { return active && ticks <= 0; }

    public void clear() {
        ticks = 0;
        active = false;
    }
}
