package dev.grimholt.server.vanilla;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class VanillaBlockRegistry {
    private final Map<String, BlockState> states = new ConcurrentHashMap<>();
    private final Map<String, VanillaBlockDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<String, Integer> stateIds = new ConcurrentHashMap<>();
    private final Map<Integer, BlockState> byStateId = new ConcurrentHashMap<>();
    private final AtomicInteger nextStateId = new AtomicInteger();

    public VanillaBlockRegistry() {
        register(new VanillaBlockDefinition("minecraft:air",0,false,false,false,false,true,Map.of()));
        String[] solid={"stone","dirt","grass_block","cobblestone","sand","gravel","oak_log","oak_planks","glass",
            "bricks","iron_block","gold_block","diamond_block","emerald_block","obsidian","bedrock","netherrack",
            "soul_sand","soul_soil","end_stone","purpur_block","deepslate","tuff","calcite","dripstone_block",
            "copper_block","amethyst_block","coal_block","redstone_block","lapis_block","quartz_block"};
        for(String id:solid) register(new VanillaBlockDefinition("minecraft:"+id,1,true,true,false,true,false,Map.of()));
        register(new VanillaBlockDefinition("minecraft:glass",0.3f,true,false,false,true,false,Map.of()));
        register(new VanillaBlockDefinition("minecraft:water",100f,false,false,true,false,true,Map.of("level","0")));
        register(new VanillaBlockDefinition("minecraft:lava",100f,false,false,true,false,true,Map.of("level","0")));
        register(new VanillaBlockDefinition("minecraft:ice",0.5f,true,false,false,true,false,Map.of()));
        register(new VanillaBlockDefinition("minecraft:packed_ice",0.5f,true,true,false,true,false,Map.of()));
        register(new VanillaBlockDefinition("minecraft:snow",0.1f,false,false,true,false,true,Map.of("layers","1")));
        register(new VanillaBlockDefinition("minecraft:powder_snow",0.25f,false,false,true,false,true,Map.of()));
        register(new VanillaBlockDefinition("minecraft:fire",0f,false,false,true,false,true,Map.of()));
        register(new VanillaBlockDefinition("minecraft:redstone_wire",0f,false,false,true,false,true,Map.of("power","0")));
        register(new VanillaBlockDefinition("minecraft:redstone_torch",0f,false,false,true,false,true,Map.of()));
        register(new VanillaBlockDefinition("minecraft:repeater",0f,false,false,false,false,true,Map.of("delay","1","powered","false")));
        register(new VanillaBlockDefinition("minecraft:comparator",0f,false,false,false,false,true,Map.of("mode","compare","powered","false")));
        register(new VanillaBlockDefinition("minecraft:piston",1.5f,true,true,false,true,false,Map.of("extended","false","facing","north")));
        register(new VanillaBlockDefinition("minecraft:sticky_piston",1.5f,true,true,false,true,false,Map.of("extended","false","facing","north")));
        register(new VanillaBlockDefinition("minecraft:observer",3f,true,true,false,true,false,Map.of("facing","north","powered","false")));
        register(new VanillaBlockDefinition("minecraft:chest",2.5f,true,true,false,true,false,Map.of("facing","north")));
        register(new VanillaBlockDefinition("minecraft:crafting_table",2.5f,true,true,false,true,false,Map.of()));
        register(new VanillaBlockDefinition("minecraft:furnace",3.5f,true,true,false,true,false,Map.of("facing","north","lit","false")));
    }

    public void register(VanillaBlockDefinition definition) {
        Objects.requireNonNull(definition);
        definitions.putIfAbsent(definition.id(), definition);
        BlockState state=definition.defaultState();
        states.putIfAbsent(definition.id(), state);
        stateIds.computeIfAbsent(stateKey(state), ignored -> {int id=nextStateId.getAndIncrement();byStateId.put(id,state);return id;});
    }
    public void registerAuthoritative(BlockState state, int id, boolean defaultState) {
        Objects.requireNonNull(state, "state");
        if (id < 0) throw new IllegalArgumentException("state id");
        VanillaBlockDefinition d = definitions.get(state.id());
        if (d == null) {
            register(new VanillaBlockDefinition(state.id(), 1, true, true, false, true, false, state.properties()));
        }
        if (defaultState) states.put(state.id(), state);
        stateIds.put(stateKey(state), id);
        byStateId.put(id, state);
        nextStateId.accumulateAndGet(id + 1, Math::max);
    }

    public void register(BlockState state) {
        Objects.requireNonNull(state);
        VanillaBlockDefinition d=definitions.get(state.id());
        if(d==null) register(new VanillaBlockDefinition(state.id(),1,true,true,false,true,false,state.properties()));
        states.putIfAbsent(state.id(),state);
        stateIds.computeIfAbsent(stateKey(state), ignored -> {int id=nextStateId.getAndIncrement();byStateId.put(id,state);return id;});
    }
    public BlockState get(String id) { return states.getOrDefault(id, states.get("minecraft:air")); }
    public BlockState defaultState(String id){return get(id);}
    public VanillaBlockDefinition definition(String id){return definitions.get(id);}
    public int stateId(BlockState state){register(state);return stateIds.get(stateKey(state));}
    public BlockState byStateId(int id){return byStateId.getOrDefault(id,states.get("minecraft:air"));}
    public boolean contains(String id){return definitions.containsKey(id);}
    public int size(){return definitions.size();}
    public int stateCount(){return stateIds.size();}
    public Map<String,BlockState> snapshot(){return Map.copyOf(states);}
    public Map<String,VanillaBlockDefinition> definitions(){return Map.copyOf(definitions);}
    private static String stateKey(BlockState s){return s.id()+"|"+s.properties();}
}
