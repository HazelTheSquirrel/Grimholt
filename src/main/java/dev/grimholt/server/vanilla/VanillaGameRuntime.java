package dev.grimholt.server.vanilla;

import java.util.UUID;

/** Region-owned deterministic Vanilla gameplay kernel. */
public final class VanillaGameRuntime {
    private final VanillaWorldModel world;
    private final VanillaWeather weather=new VanillaWeather();
    private final VanillaWorldBorder border=new VanillaWorldBorder();
    private final VanillaScoreboard scoreboard=new VanillaScoreboard();
    private final VanillaRecipeBook recipes=new VanillaRecipeBook();
    private final VanillaFluidEngine fluids=new VanillaFluidEngine();
    private final VanillaCommandDispatcher<Object> commands=new VanillaCommandDispatcher<>();
    private final VanillaGameplaySystems.GameRules gameRules=new VanillaGameplaySystems.GameRules();
    private final VanillaGameplaySystems.Attributes attributes=new VanillaGameplaySystems.Attributes();
    private final VanillaGameplaySystems.Effects effects=new VanillaGameplaySystems.Effects();
    private final VanillaGameplaySystems.Stats stats=new VanillaGameplaySystems.Stats();
    private final VanillaGameplaySystems.Advancements advancements=new VanillaGameplaySystems.Advancements();
    private final VanillaGameplaySystems.Permissions permissions=new VanillaGameplaySystems.Permissions();
    private final VanillaInteractionEngine interactions=new VanillaInteractionEngine();
    private final VanillaPersistenceModel persistence=new VanillaPersistenceModel();
    private long tick;
    public VanillaGameRuntime(UUID worldId){this(new VanillaWorldModel(worldId));}
    public VanillaGameRuntime(VanillaWorldModel worldModel){world=java.util.Objects.requireNonNull(worldModel,"worldModel");}
    public VanillaWorldModel world(){return world;} public VanillaWeather weather(){return weather;} public VanillaWorldBorder border(){return border;}
    public VanillaScoreboard scoreboard(){return scoreboard;} public VanillaRecipeBook recipes(){return recipes;} public VanillaFluidEngine fluids(){return fluids;} public VanillaCommandDispatcher<Object> commands(){return commands;}
    public VanillaGameplaySystems.GameRules gameRules(){return gameRules;} public VanillaGameplaySystems.Attributes attributes(){return attributes;} public VanillaGameplaySystems.Effects effects(){return effects;}
    public VanillaGameplaySystems.Stats stats(){return stats;} public VanillaGameplaySystems.Advancements advancements(){return advancements;} public VanillaGameplaySystems.Permissions permissions(){return permissions;}
    public VanillaInteractionEngine interactions(){return interactions;} public VanillaPersistenceModel persistence(){return persistence;}
    public long tickCount(){return tick;}
    public void tick(){tick++;weather.tick();effects.tick();}
}
