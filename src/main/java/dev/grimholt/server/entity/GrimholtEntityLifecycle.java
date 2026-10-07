package dev.grimholt.server.entity;

import dev.grimholt.server.vanilla.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class GrimholtEntityLifecycle {
    private final AtomicInteger nextNetworkId = new AtomicInteger(1);

    public int spawn(VanillaWorldModel world, VanillaEntityState entity) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(entity, "entity");
        world.addEntity(entity);
        return nextNetworkId.getAndIncrement();
    }

    public void despawn(VanillaWorldModel world, UUID uuid) {
        Objects.requireNonNull(world, "world").removeEntity(Objects.requireNonNull(uuid, "uuid"));
    }

    public Map<UUID, VanillaEntityState> snapshot(VanillaWorldModel world) {
        return world.entitySnapshot();
    }
}
