package dev.grimholt.server.vanilla;

import dev.grimholt.server.concurrency.OwnedRegion;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Region-owned Vanilla runtime. All mutation methods are required to execute
 * inside the associated OwnedRegion ownership scope.
 */
public final class VanillaRegionRuntime implements AutoCloseable {
    private final OwnedRegion owner;
    private final Map<Long, VanillaChunk> chunks = new ConcurrentHashMap<>();
    private final Map<Long, VanillaTickEngine> tickers = new ConcurrentHashMap<>();

    public VanillaRegionRuntime(OwnedRegion owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    public OwnedRegion owner() { return owner; }

    public VanillaChunk chunk(int chunkX, int chunkZ) {
        owner.assertOwner();
        long key = key(chunkX, chunkZ);
        return chunks.computeIfAbsent(key, ignored -> {
            VanillaChunk chunk = new VanillaChunk(chunkX, chunkZ);
            chunk.load();
            tickers.put(key, new VanillaTickEngine(new VanillaWorldState(), 4096));
            return chunk;
        });
    }

    public void tick() {
        owner.assertOwner();
        for (VanillaTickEngine engine : tickers.values()) engine.tick();
    }

    public int loadedChunkCount() {
        return chunks.size();
    }

    public void unloadChunk(int chunkX, int chunkZ) {
        owner.assertOwner();
        long key = key(chunkX, chunkZ);
        VanillaChunk chunk = chunks.remove(key);
        if (chunk != null) {
            chunk.unload();
            tickers.remove(key);
        }
    }

    @Override
    public void close() {
        Runnable cleanup = () -> {
            chunks.values().forEach(VanillaChunk::unload);
            chunks.clear();
            tickers.clear();
        };
        if (owner.closed()) {
            cleanup.run();
        } else if (owner.ownedByCurrentThread()) {
            cleanup.run();
        } else {
            owner.execute(cleanup);
        }
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }
}
