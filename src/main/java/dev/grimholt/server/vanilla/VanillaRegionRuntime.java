package dev.grimholt.server.vanilla;

import dev.grimholt.server.concurrency.OwnedRegion;
import java.util.*;

public final class VanillaRegionRuntime implements AutoCloseable {
    private final OwnedRegion owner;
    private final Map<Long,VanillaChunk> chunks=new HashMap<>();
    private final VanillaGameRuntime game;
    public VanillaRegionRuntime(OwnedRegion owner){this(owner,new VanillaWorldModel(owner.key().worldId()));}
    public VanillaRegionRuntime(OwnedRegion owner,VanillaWorldModel worldModel){this.owner=Objects.requireNonNull(owner);this.game=new VanillaGameRuntime(Objects.requireNonNull(worldModel));}
    public OwnedRegion owner(){return owner;} public VanillaGameRuntime game(){return game;}
    public VanillaChunk chunk(int x,int z){owner.assertOwner();return chunks.computeIfAbsent(key(x,z),k->{VanillaChunk c=new VanillaChunk(game.world(),x,z);c.load();return c;});}
    public void tick(){owner.assertOwner();game.tick();for(VanillaChunk c:chunks.values())c.tick();}
    public int loadedChunkCount(){owner.assertOwner();return chunks.size();}
    public void unloadChunk(int x,int z){owner.assertOwner();VanillaChunk c=chunks.remove(key(x,z));if(c!=null)c.unload();}
    void forceClose(){owner.close();chunks.values().forEach(VanillaChunk::unload);chunks.clear();}
    public void close(){Runnable r=()->{chunks.values().forEach(VanillaChunk::unload);chunks.clear();};if(owner.closed()||owner.ownedByCurrentThread())r.run();else owner.execute(r);}
    private static long key(int x,int z){return((long)x<<32)^(z&0xffffffffL);}
}