package dev.grimholt.server.vanilla;

import java.util.*;

/**
 * Canonical block-state catalogue. State IDs are stable for a fixed registry
 * input and every legal cartesian product of declared properties is materialized.
 */
public final class VanillaBlockStateRegistry {
    public record Property(String name, List<String> values) {
        public Property {
            Objects.requireNonNull(name);
            if (values == null || values.isEmpty()) throw new IllegalArgumentException("empty property: " + name);
            values = List.copyOf(values);
            if (new HashSet<>(values).size() != values.size()) throw new IllegalArgumentException("duplicate property value");
        }
    }

    private final VanillaBlockRegistry blocks;
    private final Map<String, List<Property>> schemas = new LinkedHashMap<>();
    private final Map<String, BlockState> byKey = new HashMap<>();
    private final Map<Integer, BlockState> byId = new HashMap<>();
    private int nextId;

    public VanillaBlockStateRegistry(VanillaBlockRegistry blocks) {
        this.blocks = Objects.requireNonNull(blocks);
    }

    /** Installs the high-value vanilla schemas used by simulation systems. */
    public void registerCoreSchemas() {
        schema("minecraft:redstone_wire",
            new Property("power", range(0, 15)),
            new Property("north", List.of("up","side","none")),
            new Property("south", List.of("up","side","none")),
            new Property("east", List.of("up","side","none")),
            new Property("west", List.of("up","side","none")));
        schema("minecraft:repeater",
            new Property("delay", range(1, 4)),
            new Property("facing", List.of("north","east","south","west")),
            new Property("locked", List.of("true","false")),
            new Property("powered", List.of("true","false")));
        schema("minecraft:comparator",
            new Property("facing", List.of("north","east","south","west")),
            new Property("mode", List.of("compare","subtract")),
            new Property("powered", List.of("true","false")));
        schema("minecraft:piston",
            new Property("extended", List.of("true","false")),
            new Property("facing", List.of("down","up","north","south","west","east")));
        schema("minecraft:sticky_piston",
            new Property("extended", List.of("true","false")),
            new Property("facing", List.of("down","up","north","south","west","east")));
        schema("minecraft:observer",
            new Property("facing", List.of("down","up","north","south","west","east")),
            new Property("powered", List.of("true","false")));
        schema("minecraft:water", new Property("level", range(0, 15)));
        schema("minecraft:lava", new Property("level", range(0, 15)));
    }

    private static List<String> range(int from, int to) {
        List<String> values = new ArrayList<>();
        for (int i = from; i <= to; i++) values.add(Integer.toString(i));
        return values;
    }

    public void schema(String blockId, Property... properties) {
        List<Property> copy = List.of(properties);
        schemas.put(blockId, copy);
        enumerate(blockId, copy, 0, new LinkedHashMap<>());
    }

    public void register(BlockState state) {
        blocks.register(state);
        int id = blocks.stateId(state);
        byKey.put(key(state), state);
        byId.putIfAbsent(id, state);
    }

    public BlockState state(String blockId, Map<String, String> properties) {
        String k = key(new BlockState(blockId, properties));
        BlockState state = byKey.get(k);
        if (state == null) throw new IllegalArgumentException("Unknown block state: " + k);
        return state;
    }

    public BlockState byId(int id) { return byId.getOrDefault(id, blocks.byStateId(id)); }
    public List<Property> schema(String blockId) { return List.copyOf(schemas.getOrDefault(blockId, List.of())); }
    public int stateCount() { return byKey.size(); }
    public Map<String, List<Property>> schemas() { return Map.copyOf(schemas); }

    private void enumerate(String id, List<Property> properties, int index, Map<String,String> values) {
        if (index == properties.size()) {
            register(new BlockState(id, values));
            return;
        }
        Property p = properties.get(index);
        for (String value : p.values()) {
            values.put(p.name(), value);
            enumerate(id, properties, index + 1, values);
        }
        values.remove(p.name());
    }

    private static String key(BlockState state) {
        StringBuilder b = new StringBuilder(state.id());
        state.properties().entrySet().stream().sorted(Map.Entry.comparingByKey())
            .forEach(e -> b.append('|').append(e.getKey()).append('=').append(e.getValue()));
        return b.toString();
    }
}
