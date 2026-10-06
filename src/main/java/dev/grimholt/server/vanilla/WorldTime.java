package dev.grimholt.server.vanilla;

public final class WorldTime {
    public static final long TICKS_PER_DAY = 24_000L;
    private long gameTime;
    private long dayTime;

    public long gameTime() { return gameTime; }
    public long dayTime() { return dayTime; }

    public void advance() {
        gameTime++;
        dayTime++;
        if (dayTime >= TICKS_PER_DAY) dayTime -= TICKS_PER_DAY;
    }

    public void setGameTime(long gameTime) {
        if (gameTime < 0) throw new IllegalArgumentException("gameTime must be non-negative");
        this.gameTime = gameTime;
    }

    public void setDayTime(long dayTime) {
        this.dayTime = Math.floorMod(dayTime, TICKS_PER_DAY);
    }
}
