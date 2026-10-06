package dev.grimholt.server.vanilla;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable item stack value. Gameplay code owns mutation by replacing the value,
 * which keeps cross-region handoffs explicit.
 */
public record VanillaItemStack(String itemId, int count, int maxStackSize, Map<String, String> components) {
    public VanillaItemStack {
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(components, "components");
        if (count < 0) throw new IllegalArgumentException("count must be non-negative");
        if (maxStackSize < 1) throw new IllegalArgumentException("maxStackSize must be positive");
        if (count > maxStackSize) throw new IllegalArgumentException("count exceeds maxStackSize");
        components = Map.copyOf(components);
    }

    public static VanillaItemStack empty() {
        return new VanillaItemStack("minecraft:air", 0, 64, Map.of());
    }

    public boolean empty() {
        return count == 0 || itemId.equals("minecraft:air");
    }

    public VanillaItemStack withCount(int newCount) {
        return new VanillaItemStack(itemId, newCount, maxStackSize, components);
    }

    public VanillaItemStack withComponent(String key, String value) {
        var next = new java.util.HashMap<>(components);
        next.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
        return new VanillaItemStack(itemId, count, maxStackSize, next);
    }
}
