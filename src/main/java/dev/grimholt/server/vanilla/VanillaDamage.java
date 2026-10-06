package dev.grimholt.server.vanilla;

import java.util.Objects;

public record VanillaDamage(String typeId, float amount) {
    public VanillaDamage {
        Objects.requireNonNull(typeId, "typeId");
        if (amount < 0) throw new IllegalArgumentException("amount must be non-negative");
    }

    public static VanillaDamage of(String typeId, float amount) {
        return new VanillaDamage(typeId, amount);
    }
}
