package dev.grimholt.server.vanilla;

import dev.grimholt.server.concurrency.OwnedRegion;
import dev.grimholt.server.concurrency.RegionManager;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Owns the Vanilla gameplay runtime for every logical region.
 *
 * <p>The manager deliberately exposes only Grimholt/Vanilla state. Minestom
 * scheduling is supplied as an execution primitive by the adapter.</p>
 */
public final class VanillaRegionManager implements AutoCloseable {
    private final RegionManager regions;
    private final Map<OwnedRegion, VanillaRegionRuntime> runtimes = new ConcurrentHashMap<>();

    public VanillaRegionManager(int maxHandoffs,
                                Consumer<Runnable> scheduler,
                                Consumer<Throwable> failureHandler) {
        regions = new RegionManager(
                maxHandoffs,
                Objects.requireNonNull(scheduler, "scheduler"),
                Objects.requireNonNull(failureHandler, "failureHandler"));
    }

    public VanillaRegionRuntime region(UUID worldId, int chunkX, int chunkZ) {
        OwnedRegion owner = regions.region(worldId, chunkX, chunkZ);
        return runtimes.computeIfAbsent(owner, VanillaRegionRuntime::new);
    }

    public VanillaRegionRuntime region(UUID worldId, VanillaWorldModel worldModel, int chunkX, int chunkZ) {
        Objects.requireNonNull(worldModel, "worldModel");
        if (!worldId.equals(worldModel.worldId())) throw new IllegalArgumentException("World model UUID does not match region UUID");
        OwnedRegion owner = regions.region(worldId, chunkX, chunkZ);
        return runtimes.computeIfAbsent(owner, ignored -> new VanillaRegionRuntime(owner, worldModel));
    }

    public void execute(UUID worldId, int chunkX, int chunkZ, Runnable action) {
        region(worldId, chunkX, chunkZ).owner().execute(action);
    }

    public int regionCount() {
        return regions.regionCount();
    }

    /** Schedules one deterministic gameplay tick for every currently live region. */
    public void tickAll() {
        for (VanillaRegionRuntime runtime : runtimes.values()) {
            try {
                runtime.requestTick();
            } catch (java.util.concurrent.RejectedExecutionException ignored) {
                // Region shutdown races are intentionally harmless.
            }
        }
    }

    public void closeRegion(UUID worldId, int regionX, int regionZ) {
        OwnedRegion owner = regions.find(worldId, regionX, regionZ);
        if (owner == null) return;
        VanillaRegionRuntime runtime = runtimes.remove(owner);
        if (runtime != null) runtime.close();
        regions.closeRegion(worldId, regionX, regionZ);
    }

    @Override
    public void close() {
        runtimes.values().forEach(VanillaRegionRuntime::forceClose);
        runtimes.clear();
        regions.close();
    }
}
