package dev.grimholt.server.vanilla;

public final class VanillaWorldRules {
    public static final int MIN_Y=-64;
    public static final int MAX_Y=319;
    private boolean daylightCycle=true;
    private boolean mobGriefing=true;
    private boolean doMobSpawning=true;
    public boolean daylightCycle(){return daylightCycle;} public void daylightCycle(boolean v){daylightCycle=v;}
    public boolean mobGriefing(){return mobGriefing;} public void mobGriefing(boolean v){mobGriefing=v;}
    public boolean doMobSpawning(){return doMobSpawning;} public void doMobSpawning(boolean v){doMobSpawning=v;}
    public boolean validY(int y){return y>=MIN_Y&&y<=MAX_Y;}
}
