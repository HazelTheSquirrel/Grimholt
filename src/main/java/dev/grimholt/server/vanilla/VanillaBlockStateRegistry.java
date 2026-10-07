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
