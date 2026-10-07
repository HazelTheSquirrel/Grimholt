package dev.grimholt.server.vanilla;

import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Numeric protocol IDs loaded from Mojang's exact registry report.
 */
public final class VanillaRegistryIds {
    private final Map<String,Integer> ids = new ConcurrentHashMap<>();

    public void register(String id, int protocolId) {
        if (id == null || protocolId < 0) throw new IllegalArgumentException();
        ids.put(id, protocolId);
    }

    public OptionalInt id(String id) {
        Integer value = ids.get(id);
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }

    public int requireId(String id) {
        return id(id).orElseThrow(() -> new IllegalArgumentException("Unknown registry id: " + id));
    }

    public int size() { return ids.size(); }
    public Map<String,Integer> snapshot() { return Map.copyOf(ids); }
}
