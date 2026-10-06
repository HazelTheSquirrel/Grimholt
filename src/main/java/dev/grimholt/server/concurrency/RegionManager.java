package dev.grimholt.server.concurrency;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

public final class RegionManager implements AutoCloseable {
    public static final int CHUNKS_PER_REGION = 8;

    private final Map<RegionKey, OwnedRegion> regions = new ConcurrentHashMap<>();
    private final int maxHandoffs;
    private final Consumer<Runnable> scheduler;
    private final Consumer<Throwable> failureHandler;

    public RegionManager(int maxHandoffs, Consumer<Runnable> scheduler, Consumer<Throwable> failureHandler) {
        if (maxHandoffs < 1) throw new IllegalArgumentException("maxHandoffs must be positive");
        this.maxHandoffs = maxHandoffs;
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.failureHandler = Objects.requireNonNull(failureHandler, "failureHandler");
    }

    public OwnedRegion region(UUID worldId, int chunkX, int chunkZ) {
        RegionKey key = new RegionKey(worldId, Math.floorDiv(chunkX, CHUNKS_PER_REGION),
                Math.floorDiv(chunkZ, CHUNKS_PER_REGION));
        return regions.computeIfAbsent(key,
                k -> new OwnedRegion(k, maxHandoffs, scheduler, failureHandler));
    }

    public int regionCount() { return regions.size(); }

    public OwnedRegion find(UUID worldId, int regionX, int regionZ) {
        return regions.get(new RegionKey(worldId, regionX, regionZ));
    }

    public void execute(UUID worldId, int chunkX, int chunkZ, Runnable action) {
        OwnedRegion region = region(worldId, chunkX, chunkZ);
        try {
            region.execute(action);
        } catch (RejectedExecutionException e) {
            throw e;
        }
    }

    public void closeRegion(UUID worldId, int regionX, int regionZ) {
        OwnedRegion region = regions.remove(new RegionKey(worldId, regionX, regionZ));
        if (region != null) region.close();
    }

    @Override
    public void close() {
        regions.values().forEach(OwnedRegion::close);
        regions.clear();
    }
}
