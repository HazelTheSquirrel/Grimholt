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
    private final VanillaGameRuntime game;

    public VanillaRegionRuntime(OwnedRegion owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.game = new VanillaGameRuntime(owner.key().worldId());
    }

    public OwnedRegion owner() { return owner; }
    public VanillaGameRuntime game() { return game; }

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
        game.tick();
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

    /** Used only after the owning region has been closed; no other owner can mutate the maps. */
    void forceClose() {
        owner.close();
        chunks.values().forEach(VanillaChunk::unload);
        chunks.clear();
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
