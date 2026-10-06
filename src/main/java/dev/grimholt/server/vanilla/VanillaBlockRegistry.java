package dev.grimholt.server.vanilla;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class VanillaBlockRegistry {
    private final Map<String, BlockState> states = new ConcurrentHashMap<>();

    public VanillaBlockRegistry() {
        register(BlockState.of("minecraft:air"));
        register(BlockState.of("minecraft:stone"));
        register(BlockState.of("minecraft:dirt"));
        register(BlockState.of("minecraft:water"));
        register(BlockState.of("minecraft:lava"));
        register(BlockState.of("minecraft:ice"));
        register(BlockState.of("minecraft:packed_ice"));
        register(BlockState.of("minecraft:snow"));
        register(BlockState.of("minecraft:powder_snow"));
    }

    public void register(BlockState state) {
        Objects.requireNonNull(state, "state");
        states.putIfAbsent(state.id(), state);
    }

    public BlockState get(String id) {
        Objects.requireNonNull(id, "id");
        return states.getOrDefault(id, BlockState.of("minecraft:air"));
    }

    public boolean contains(String id) {
        return states.containsKey(id);
    }

    public int size() { return states.size(); }

    public Map<String, BlockState> snapshot() { return Map.copyOf(states); }
}
