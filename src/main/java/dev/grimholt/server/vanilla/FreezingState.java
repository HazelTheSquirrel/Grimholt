package dev.grimholt.server.vanilla;

/**
 * State machine for the 26.4 Freezing effect.
 *
 * <p>The state deliberately exposes damage timing without performing entity
 * mutation. The owning entity system decides how damage and leather protection
 * are applied.</p>
 */
public final class FreezingState {
    public static final int FREEZE_THRESHOLD_TICKS = 140;
    public static final int DAMAGE_INTERVAL_TICKS = 40;

    private int remainingTicks;
    private int frozenTicks;

    public void apply(int durationTicks) {
        if (durationTicks < 0) throw new IllegalArgumentException("durationTicks must be non-negative");
        remainingTicks = Math.max(remainingTicks, durationTicks);
    }

    public boolean active() { return remainingTicks > 0; }
    public int remainingTicks() { return remainingTicks; }
    public int frozenTicks() { return frozenTicks; }
    public boolean fullyFrozen() { return frozenTicks >= FREEZE_THRESHOLD_TICKS; }

    /**
     * Advances one game tick and returns whether a freezing damage pulse is due.
     */
    public boolean tick() {
        if (!active()) {
            frozenTicks = 0;
            return false;
        }
        remainingTicks--;
        frozenTicks++;
        return fullyFrozen() && (frozenTicks - FREEZE_THRESHOLD_TICKS) % DAMAGE_INTERVAL_TICKS == 0;
    }

    public void clear() {
        remainingTicks = 0;
        frozenTicks = 0;
    }
}
