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

    public void execute(UUID worldId, int chunkX, int chunkZ, Runnable action) {
        region(worldId, chunkX, chunkZ).owner().execute(action);
    }

    public int regionCount() {
        return regions.regionCount();
    }

    public void closeRegion(UUID worldId, int regionX, int regionZ) {
        OwnedRegion owner = regions.region(worldId, regionX * RegionManager.CHUNKS_PER_REGION,
                regionZ * RegionManager.CHUNKS_PER_REGION);
        VanillaRegionRuntime runtime = runtimes.remove(owner);
        if (runtime != null) runtime.close();
        regions.closeRegion(worldId, regionX, regionZ);
    }

    @Override
    public void close() {
        runtimes.values().forEach(VanillaRegionRuntime::close);
        runtimes.clear();
        regions.close();
    }
}
