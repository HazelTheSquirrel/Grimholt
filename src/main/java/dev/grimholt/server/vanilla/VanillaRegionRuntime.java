package dev.grimholt.server.vanilla;

import dev.grimholt.server.concurrency.OwnedRegion;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class VanillaRegionRuntime implements AutoCloseable {
    private final OwnedRegion owner;
    private final Map<Long,VanillaChunk> chunks=new HashMap<>();
    private final VanillaGameRuntime game;
    private final AtomicBoolean tickQueued = new AtomicBoolean();
    public VanillaRegionRuntime(OwnedRegion owner){this(owner,new VanillaWorldModel(owner.key().worldId()));}
    public VanillaRegionRuntime(OwnedRegion owner,VanillaWorldModel worldModel){this.owner=Objects.requireNonNull(owner);this.game=new VanillaGameRuntime(Objects.requireNonNull(worldModel));}
    public OwnedRegion owner(){return owner;} public VanillaGameRuntime game(){return game;}
    public VanillaChunk chunk(int x,int z){owner.assertOwner();return chunks.computeIfAbsent(key(x,z),k->{VanillaChunk c=new VanillaChunk(game.world(),x,z);c.load();return c;});}
    public void tick(){owner.assertOwner();game.tick();for(VanillaChunk c:chunks.values())c.tick();}

    /**
     * Request one region tick without allowing a slow region to accumulate an
     * unbounded backlog of obsolete tick tasks. A pending tick is coalesced;
     * the region's owner still serializes the actual mutation.
     *
     * @return true if a tick was queued or executed, false if one is already pending
     */
    public boolean requestTick() {
        if (!tickQueued.compareAndSet(false, true)) return false;
        try {
            owner.execute(() -> {
                try {
                    tick();
                } finally {
                    tickQueued.set(false);
                }
            });
            return true;
        } catch (RuntimeException | Error failure) {
            tickQueued.set(false);
            throw failure;
        }
    }

    public boolean tickPending() { return tickQueued.get(); }
    public int loadedChunkCount(){owner.assertOwner();return chunks.size();}
    public void unloadChunk(int x,int z){owner.assertOwner();VanillaChunk c=chunks.remove(key(x,z));if(c!=null)c.unload();}
    void forceClose(){owner.close();chunks.values().forEach(VanillaChunk::unload);chunks.clear();}
    public void close(){Runnable r=()->{chunks.values().forEach(VanillaChunk::unload);chunks.clear();};if(owner.closed()||owner.ownedByCurrentThread())r.run();else owner.execute(r);}
    private static long key(int x,int z){return((long)x<<32)^(z&0xffffffffL);}
}