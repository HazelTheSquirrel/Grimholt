package dev.grimholt.server.vanilla;

import java.util.UUID;

/** Region-owned gameplay kernel: deterministic order for world systems and player/entity state. */
public final class VanillaGameRuntime {
    private final VanillaWorldModel world;
    private final VanillaWeather weather=new VanillaWeather();
    private final VanillaWorldBorder border=new VanillaWorldBorder();
    private final VanillaScoreboard scoreboard=new VanillaScoreboard();
    private final VanillaRecipeBook recipes=new VanillaRecipeBook();
    private final VanillaFluidEngine fluids=new VanillaFluidEngine();
    private final VanillaCommandDispatcher<Object> commands=new VanillaCommandDispatcher<>();
    private long tick;
    public VanillaGameRuntime(UUID worldId){world=new VanillaWorldModel(worldId);}
    public VanillaWorldModel world(){return world;} public VanillaWeather weather(){return weather;} public VanillaWorldBorder border(){return border;}
    public VanillaScoreboard scoreboard(){return scoreboard;} public VanillaRecipeBook recipes(){return recipes;} public VanillaFluidEngine fluids(){return fluids;} public VanillaCommandDispatcher<Object> commands(){return commands;}
    public long tickCount(){return tick;}
    public void tick(){tick++;weather.tick();}
}
