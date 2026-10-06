package dev.grimholt.server.vanilla;

import java.util.Map;
import java.util.Objects;

public record BlockState(String id, Map<String, String> properties) {
    public BlockState {
        Objects.requireNonNull(id, "id");
        properties = Map.copyOf(properties == null ? Map.of() : properties);
    }

    public static BlockState of(String id) {
        return new BlockState(id, Map.of());
    }

    public String property(String name) {
        return properties.get(name);
    }
}
