package dev.grimholt.server.vanilla;

import java.util.Objects;

public record VanillaFluidState(String id, int level, boolean falling) {
    public VanillaFluidState {
        Objects.requireNonNull(id, "id");
        if (level < 0 || level > 8) throw new IllegalArgumentException("level must be 0..8");
    }

    public static VanillaFluidState empty() {
        return new VanillaFluidState("minecraft:empty", 0, false);
    }

    public boolean empty() {
        return id.equals("minecraft:empty");
    }

    public boolean source() {
        return !empty() && level == 0;
    }
}
