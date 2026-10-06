package dev.grimholt.server.vanilla;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class VanillaEntityRegistry {
    private final Map<String, String> types = new ConcurrentHashMap<>();

    public VanillaEntityRegistry() {
        register("minecraft:player");
        register("minecraft:zombie");
        register("minecraft:husk");
        register("minecraft:skeleton");
        register("minecraft:creeper");
        register("minecraft:spider");
        register("minecraft:frostbite");
        register("minecraft:item");
    }

    public void register(String typeId) {
        Objects.requireNonNull(typeId, "typeId");
        types.putIfAbsent(typeId, typeId);
    }

    public boolean contains(String typeId) { return types.containsKey(typeId); }
    public int size() { return types.size(); }
    public Map<String, String> snapshot() { return Map.copyOf(types); }
}
