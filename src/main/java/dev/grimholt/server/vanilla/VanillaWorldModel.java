package dev.grimholt.server.vanilla;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VanillaWorldModel {
    private final UUID worldId;
    private final VanillaWorldState blocks = new VanillaWorldState();
    private final VanillaBlockRegistry blockRegistry = new VanillaBlockRegistry();
    private final VanillaEntityRegistry entityRegistry = new VanillaEntityRegistry();
    private final Map<UUID, VanillaEntityState> entities = new ConcurrentHashMap<>();

    public VanillaWorldModel(UUID worldId) {
        this.worldId = java.util.Objects.requireNonNull(worldId, "worldId");
    }

    public UUID worldId() { return worldId; }
    public VanillaWorldState blocks() { return blocks; }
    public VanillaBlockRegistry blockRegistry() { return blockRegistry; }
    public VanillaEntityRegistry entityRegistry() { return entityRegistry; }

    public void addEntity(VanillaEntityState entity) {
        if (!entityRegistry.contains(entity.typeId())) throw new IllegalArgumentException("Unknown entity type: " + entity.typeId());
        entities.put(entity.uuid(), entity);
    }

    public VanillaEntityState entity(UUID uuid) { return entities.get(uuid); }
    public void removeEntity(UUID uuid) { entities.remove(uuid); }
    public int entityCount() { return entities.size(); }
    public Map<UUID, VanillaEntityState> entitySnapshot() { return Map.copyOf(entities); }
}
