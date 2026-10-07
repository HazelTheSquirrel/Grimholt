package dev.grimholt.server.vanilla;

import java.util.UUID;

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
    private final VanillaParityRuntime.DataComponents dataComponents=new VanillaParityRuntime.DataComponents();
    private final VanillaParityRuntime.ContainerProtocol containerProtocol=new VanillaParityRuntime.ContainerProtocol();
    private final VanillaParityRuntime.RecipeSystems recipeSystems=new VanillaParityRuntime.RecipeSystems();
    private final VanillaParityRuntime.EntitySystem entitySystem=new VanillaParityRuntime.EntitySystem();
    private final VanillaParityRuntime.MobAi mobAi=new VanillaParityRuntime.MobAi();
    private final VanillaParityRuntime.Combat combat=new VanillaParityRuntime.Combat();
    private final VanillaParityRuntime.PoiAndVillagers villagers=new VanillaParityRuntime.PoiAndVillagers();
    private final VanillaParityRuntime.Raids raids=new VanillaParityRuntime.Raids();
    private final VanillaParityRuntime.Dimensions dimensions=new VanillaParityRuntime.Dimensions();
    private final VanillaParityRuntime.Loot loot=new VanillaParityRuntime.Loot();
    private final VanillaParityRuntime.Advancements advancementData=new VanillaParityRuntime.Advancements();
    private final VanillaParityRuntime.DataPacks dataPacks=new VanillaParityRuntime.DataPacks();
    private final VanillaParityRuntime.Commands vanillaCommands=new VanillaParityRuntime.Commands();
    private final VanillaParityRuntime.Persistence persistenceV2=new VanillaParityRuntime.Persistence();
    private final VanillaParityRuntime.NetworkParity networkParity=new VanillaParityRuntime.NetworkParity();
    private final VanillaParityRuntime.ReferenceTests referenceTests=new VanillaParityRuntime.ReferenceTests();
    private final VanillaParityRuntime.Stress stress=new VanillaParityRuntime.Stress();
    private final VanillaPhysicsEngine physics=new VanillaPhysicsEngine();
    private final VanillaBlockInteraction blockInteraction=new VanillaBlockInteraction();
    private final VanillaFluidSimulation fluidSimulation=new VanillaFluidSimulation();
    private final VanillaRedstoneEngine redstone=new VanillaRedstoneEngine();
    private final VanillaCraftingEngine crafting=new VanillaCraftingEngine();
    private long tick;
    public VanillaGameRuntime(UUID worldId){this(new VanillaWorldModel(worldId));}
    public VanillaGameRuntime(VanillaWorldModel worldModel){world=java.util.Objects.requireNonNull(worldModel);crafting.registerDefaults();recipeSystems.registerDefaults();vanillaCommands.registerVanillaRoots();}
    public VanillaWorldModel world(){return world;} public VanillaWeather weather(){return weather;} public VanillaWorldBorder border(){return border;} public VanillaScoreboard scoreboard(){return scoreboard;}
    public VanillaRecipeBook recipes(){return recipes;} public VanillaFluidEngine fluids(){return fluids;} public VanillaCommandDispatcher<Object> commands(){return commands;}
    public VanillaGameplaySystems.GameRules gameRules(){return gameRules;} public VanillaGameplaySystems.Attributes attributes(){return attributes;} public VanillaGameplaySystems.Effects effects(){return effects;} public VanillaGameplaySystems.Stats stats(){return stats;} public VanillaGameplaySystems.Advancements advancements(){return advancements;} public VanillaGameplaySystems.Permissions permissions(){return permissions;}
    public VanillaInteractionEngine interactions(){return interactions;} public VanillaPersistenceModel persistence(){return persistence;}
    public VanillaParityRuntime.DataComponents dataComponents(){return dataComponents;} public VanillaParityRuntime.ContainerProtocol containerProtocol(){return containerProtocol;} public VanillaParityRuntime.RecipeSystems recipeSystems(){return recipeSystems;}
    public VanillaParityRuntime.EntitySystem entitySystem(){return entitySystem;} public VanillaParityRuntime.MobAi mobAi(){return mobAi;} public VanillaParityRuntime.Combat combat(){return combat;} public VanillaParityRuntime.PoiAndVillagers villagers(){return villagers;} public VanillaParityRuntime.Raids raids(){return raids;} public VanillaParityRuntime.Dimensions dimensions(){return dimensions;} public VanillaParityRuntime.Loot loot(){return loot;} public VanillaParityRuntime.Advancements advancementData(){return advancementData;} public VanillaParityRuntime.DataPacks dataPacks(){return dataPacks;} public VanillaParityRuntime.Commands vanillaCommands(){return vanillaCommands;} public VanillaParityRuntime.Persistence persistenceV2(){return persistenceV2;} public VanillaParityRuntime.NetworkParity networkParity(){return networkParity;} public VanillaParityRuntime.ReferenceTests referenceTests(){return referenceTests;} public VanillaParityRuntime.Stress stress(){return stress;}
    public VanillaPhysicsEngine physics(){return physics;} public VanillaBlockInteraction blockInteraction(){return blockInteraction;} public VanillaFluidSimulation fluidSimulation(){return fluidSimulation;} public VanillaRedstoneEngine redstone(){return redstone;} public VanillaCraftingEngine crafting(){return crafting;}
    public long tickCount(){return tick;}
    public void tick(){tick++;weather.tick();effects.tick();fluidSimulation.tick(world,4096);redstone.tick(world,4096);}
}