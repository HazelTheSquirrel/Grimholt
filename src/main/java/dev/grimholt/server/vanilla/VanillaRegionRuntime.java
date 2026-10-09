package dev.grimholt.server.vanilla;

import dev.grimholt.server.concurrency.OwnedRegion;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class VanillaRegionRuntime implements AutoCloseable {
    private final OwnedRegion owner;
    private final Map<Long,VanillaChunk> chunks=new HashMap<>();
    private final VanillaGameRuntime game;
    private final VanillaOverworldGenerator overworldGenerator;
    private final AtomicBoolean tickQueued = new AtomicBoolean();
    public VanillaRegionRuntime(OwnedRegion owner){this(owner,new VanillaWorldModel(owner.key().worldId()));}
    public VanillaRegionRuntime(OwnedRegion owner,VanillaWorldModel worldModel){
        this.owner=Objects.requireNonNull(owner);
        this.game=new VanillaGameRuntime(Objects.requireNonNull(worldModel));
        this.overworldGenerator=new VanillaOverworldGenerator(worldModel.seed());
    }
    public OwnedRegion owner(){return owner;} public VanillaGameRuntime game(){return game;}
    public VanillaChunk chunk(int x,int z){
        owner.assertOwner();
        return chunks.computeIfAbsent(key(x,z),k->{
            VanillaChunk c=new VanillaChunk(game.world(),x,z);
            generateBootstrapTerrain(c);
            c.load();
            return c;
        });
    }

    /**
     * Produces a real, safe starter surface while the full noise/aquifer/feature
     * pipeline is still under development. Terrain is deterministic for the
     * configured world seed and uses the authoritative block-state registry.
     */
    private void generateBootstrapTerrain(VanillaChunk chunk) {
        VanillaBlockRegistry blocks=game.world().blockRegistry();
        BlockState bedrock=blocks.defaultState("minecraft:bedrock");
        BlockState stone=blocks.defaultState("minecraft:stone");
        BlockState dirt=blocks.defaultState("minecraft:dirt");
        BlockState grass=blocks.defaultState("minecraft:grass_block");
        for(int z=0;z<16;z++) {
            for(int x=0;x<16;x++) {
                int worldX=chunk.chunkX()*16+x;
                int worldZ=chunk.chunkZ()*16+z;
                int surface=overworldGenerator.surfaceY(worldX,worldZ);
                chunk.setBlock(new BlockPos(worldX,VanillaChunk.MIN_SECTION_Y*16,worldZ),bedrock);
                for(int y=VanillaChunk.MIN_SECTION_Y*16+1;y<surface-3;y++) {
                    chunk.setBlock(new BlockPos(worldX,y,worldZ),stone);
                }
                for(int y=surface-3;y<surface;y++) {
                    chunk.setBlock(new BlockPos(worldX,y,worldZ),dirt);
                }
                chunk.setBlock(new BlockPos(worldX,surface,worldZ),grass);
            }
        }
    }
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