package dev.grimholt.server.vanilla;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class VanillaWorldModel {
    private final UUID worldId;
    private final VanillaWorldState legacyBlocks = new VanillaWorldState();
    private final VanillaBlockRegistry blockRegistry = new VanillaBlockRegistry();
    private final VanillaBlockStateRegistry blockStates = new VanillaBlockStateRegistry(blockRegistry);
    private final VanillaItemRegistry itemRegistry = new VanillaItemRegistry();
    private final VanillaEntityRegistry entityRegistry = new VanillaEntityRegistry();
    private final Map<Long,VanillaChunk> chunks = new HashMap<>();
    private final Map<BlockPos,VanillaFluidState> fluids = new HashMap<>();
    private final Map<UUID,VanillaEntityState> entities = new ConcurrentHashMap<>();
    private long seed;
    private VanillaGameplaySystems.Dimension dimension=VanillaGameplaySystems.Dimension.OVERWORLD;

    public VanillaWorldModel(UUID worldId){this.worldId=Objects.requireNonNull(worldId);blockStates.registerCoreSchemas();}
    public UUID worldId(){return worldId;}
    public VanillaWorldState blocks(){return legacyBlocks;}
    public VanillaBlockRegistry blockRegistry(){return blockRegistry;}
    public VanillaBlockStateRegistry blockStates(){return blockStates;}
    public VanillaItemRegistry itemRegistry(){return itemRegistry;}
    public VanillaEntityRegistry entityRegistry(){return entityRegistry;}
    public long seed(){return seed;}
    public void seed(long seed){this.seed=seed;}
    public VanillaGameplaySystems.Dimension dimension(){return dimension;}
    public void dimension(VanillaGameplaySystems.Dimension d){dimension=Objects.requireNonNull(d);}

    public VanillaChunk chunk(int chunkX,int chunkZ){
        long k=key(chunkX,chunkZ);
        return chunks.computeIfAbsent(k, ignored -> new VanillaChunk(this,chunkX,chunkZ));
    }
    public Collection<VanillaChunk> chunks(){return List.copyOf(chunks.values());}
    public BlockState getBlock(BlockPos p){
        VanillaChunk c=chunk(Math.floorDiv(p.x(),16),Math.floorDiv(p.z(),16));
        return c.block(p);
    }
    public void setBlock(BlockPos p,BlockState s){
        chunk(Math.floorDiv(p.x(),16),Math.floorDiv(p.z(),16)).setBlock(p,s);
    }
    public VanillaFluidState getFluid(BlockPos p){return fluids.getOrDefault(p,VanillaFluidState.empty());}
    public void setFluid(BlockPos p,VanillaFluidState s){if(s.isEmpty())fluids.remove(p);else fluids.put(p,s);}
    public Set<BlockPos> fluidPositions(){return Set.copyOf(fluids.keySet());}

    public void addEntity(VanillaEntityState e){if(!entityRegistry.contains(e.typeId()))throw new IllegalArgumentException("Unknown entity type: "+e.typeId());entities.put(e.uuid(),e);}
    public VanillaEntityState entity(UUID id){return entities.get(id);}
    public void removeEntity(UUID id){entities.remove(id);}
    public int entityCount(){return entities.size();}
    public Map<UUID,VanillaEntityState> entitySnapshot(){return Map.copyOf(entities);}

    private static long key(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
}
