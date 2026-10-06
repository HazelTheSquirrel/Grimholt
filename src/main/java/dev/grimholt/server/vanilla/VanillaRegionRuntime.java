package dev.grimholt.server.vanilla;

import dev.grimholt.server.concurrency.OwnedRegion;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Region-owned Vanilla runtime. All gameplay mutation is serialized by the
 * associated OwnedRegion.
 */
public final class VanillaRegionRuntime implements AutoCloseable {
    private final OwnedRegion owner;
    private final Map<Long, VanillaChunk> chunks = new HashMap<>();

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
            return chunk;
        });
    }

    public void tick() {
        owner.assertOwner();
        for (VanillaChunk chunk : chunks.values()) chunk.tick();
    }

    public int loadedChunkCount() {
        owner.assertOwner();
        return chunks.size();
    }

    public void unloadChunk(int chunkX, int chunkZ) {
        owner.assertOwner();
        VanillaChunk chunk = chunks.remove(key(chunkX, chunkZ));
        if (chunk != null) chunk.unload();
    }

    @Override
    public void close() {
        Runnable cleanup = () -> {
            chunks.values().forEach(VanillaChunk::unload);
            chunks.clear();
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
