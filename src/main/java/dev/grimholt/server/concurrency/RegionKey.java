package dev.grimholt.server.concurrency;

/**
 * Stable logical ownership key for a region of world state.
 *
 * <p>The key is deliberately independent of executor threads. A region may
 * migrate between worker threads while retaining the same logical owner.</p>
 */
public record RegionKey(java.util.UUID worldId, int regionX, int regionZ) {
    public RegionKey {
        java.util.Objects.requireNonNull(worldId, "worldId");
    }
}
