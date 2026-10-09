package dev.grimholt.server.vanilla;

import dev.grimholt.server.concurrency.OwnedRegion;
import dev.grimholt.server.concurrency.RegionManager;
import dev.grimholt.server.runtime.ChronosRegionScheduler;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Owns the Vanilla gameplay runtime for every logical region.
 *
 * <p>The legacy constructor accepts an executor for compatibility. The Chronos
 * constructor routes both region ticks and ownership handoffs through Grimholt's
 * bounded worker pool, while every mutation remains guarded by OwnedRegion.</p>
 */
public final class VanillaRegionManager implements AutoCloseable {
    private final RegionManager regions;
    private final Map<OwnedRegion, VanillaRegionRuntime> runtimes = new ConcurrentHashMap<>();
    private final Map<OwnedRegion, ChronosRegionScheduler.Registration> tickRegistrations =
            new ConcurrentHashMap<>();
    private final ChronosRegionScheduler chronos;
    private final Consumer<Throwable> failureHandler;

    public VanillaRegionManager(int maxHandoffs,
                                Consumer<Runnable> scheduler,
                                Consumer<Throwable> failureHandler) {
        this.chronos = null;
        this.failureHandler = Objects.requireNonNull(failureHandler, "failureHandler");
        regions = new RegionManager(maxHandoffs,
                Objects.requireNonNull(scheduler, "scheduler"), this.failureHandler);
    }

    /**
     * Creates a native Chronos-backed region manager. Call startChronos() after
     * world/bootstrap initialization to begin the automatic 20-TPS dispatch.
     * Do not also drive this manager with tickAll().
     */
    public VanillaRegionManager(int maxHandoffs,
                                ChronosRegionScheduler chronos,
                                Consumer<Throwable> failureHandler) {
        this.chronos = Objects.requireNonNull(chronos, "chronos");
        this.failureHandler = Objects.requireNonNull(failureHandler, "failureHandler");
        regions = new RegionManager(maxHandoffs, chronos::execute, this.failureHandler);
    }

    public VanillaRegionRuntime region(UUID worldId, int chunkX, int chunkZ) {
        OwnedRegion owner = regions.region(worldId, chunkX, chunkZ);
        return runtime(owner, () -> new VanillaRegionRuntime(owner));
    }

    public VanillaRegionRuntime region(UUID worldId, VanillaWorldModel worldModel, int chunkX, int chunkZ) {
        Objects.requireNonNull(worldModel, "worldModel");
        if (!worldId.equals(worldModel.worldId())) {
            throw new IllegalArgumentException("World model UUID does not match region UUID");
        }
        OwnedRegion owner = regions.region(worldId, chunkX, chunkZ);
        return runtime(owner, () -> new VanillaRegionRuntime(owner, worldModel));
    }

    private VanillaRegionRuntime runtime(OwnedRegion owner, Supplier<VanillaRegionRuntime> factory) {
        return runtimes.computeIfAbsent(owner, key -> {
            VanillaRegionRuntime runtime = factory.get();
            if (chronos != null) {
                ChronosRegionScheduler.Registration registration =
                        chronos.register(key.key().toString(), runtime::tickOwned);
                registration.onFailure(failureHandler);
                tickRegistrations.put(key, registration);
            }
            return runtime;
        });
    }

    public void execute(UUID worldId, int chunkX, int chunkZ, Runnable action) {
        region(worldId, chunkX, chunkZ).owner().execute(action);
    }

    public int regionCount() {
        return regions.regionCount();
    }

    /** Starts automatic region ticking when this manager was constructed with Chronos. */
    public void startChronos() {
        if (chronos == null) {
            throw new IllegalStateException("This region manager was not configured with Chronos");
        }
        chronos.start();
    }

    /**
     * Compatibility path for externally clocked managers. Chronos-backed managers
     * already dispatch each registered region and reject this call to avoid double ticks.
     */
    public void tickAll() {
        if (chronos != null) {
            throw new IllegalStateException("Chronos-backed managers tick automatically; do not call tickAll()");
        }
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
        ChronosRegionScheduler.Registration registration = tickRegistrations.remove(owner);
        if (registration != null) registration.close();
        VanillaRegionRuntime runtime = runtimes.remove(owner);
        if (runtime != null) runtime.close();
        regions.closeRegion(worldId, regionX, regionZ);
    }

    @Override
    public void close() {
        tickRegistrations.values().forEach(ChronosRegionScheduler.Registration::close);
        tickRegistrations.clear();
        runtimes.values().forEach(VanillaRegionRuntime::forceClose);
        runtimes.clear();
        regions.close();
    }
}
