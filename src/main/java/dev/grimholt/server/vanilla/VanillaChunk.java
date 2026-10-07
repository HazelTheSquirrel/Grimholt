package dev.grimholt.server.vanilla;

import java.util.*;
/** Region-owned chunk with real 24-section (16x16x16) storage. */
public final class VanillaChunk {
    public static final int MIN_SECTION_Y=-4, MAX_SECTION_Y=19;
    private final VanillaWorldModel worldModel;
    private final int chunkX,chunkZ;
    private final Map<Integer,VanillaChunkSection> sections=new HashMap<>();
    private final Map<BlockPos,VanillaFluidState> fluids=new HashMap<>();
    private final VanillaTickEngine ticker;
    private boolean loaded;

    public VanillaChunk(int chunkX,int chunkZ){this(new VanillaWorldModel(UUID.randomUUID()),chunkX,chunkZ);}
    public VanillaChunk(VanillaWorldModel world,int chunkX,int chunkZ){this.worldModel=Objects.requireNonNull(world);this.chunkX=chunkX;this.chunkZ=chunkZ;this.ticker=new VanillaTickEngine(world.blocks(),4096);}
    public int chunkX(){return chunkX;} public int chunkZ(){return chunkZ;} public boolean loaded(){return loaded;}
    public void load(){loaded=true;} public void unload(){loaded=false;}
    public VanillaWorldModel worldModel(){return worldModel;}
    public BlockState block(BlockPos p){
        int sy=Math.floorDiv(p.y(),16), ly=Math.floorMod(p.y(),16);
        if(sy<MIN_SECTION_Y||sy>MAX_SECTION_Y)return BlockState.of("minecraft:air");
        VanillaChunkSection s=sections.get(sy);
        if(s==null)return worldModel.blockRegistry().defaultState("minecraft:air");
        return worldModel.blockRegistry().byStateId(s.stateId(Math.floorMod(p.x(),16),ly,Math.floorMod(p.z(),16)));
    }
    public void setBlock(BlockPos p,BlockState state){
        int sy=Math.floorDiv(p.y(),16),ly=Math.floorMod(p.y(),16);
        if(sy<MIN_SECTION_Y||sy>MAX_SECTION_Y)throw new IllegalArgumentException("Y outside 26.x overworld range");
        int id=worldModel.blockRegistry().stateId(state);
        VanillaChunkSection s=sections.computeIfAbsent(sy,k->new VanillaChunkSection(k,worldModel.blockRegistry().stateId(worldModel.blockRegistry().defaultState("minecraft:air"))));
        s.stateId(Math.floorMod(p.x(),16),ly,Math.floorMod(p.z(),16),id,worldModel.blockRegistry().stateId(worldModel.blockRegistry().defaultState("minecraft:air")));
    }
    public VanillaFluidState fluid(BlockPos p){return fluids.getOrDefault(p,worldModel.getFluid(p));}
    public void setFluid(BlockPos p,VanillaFluidState s){if(s.isEmpty())fluids.remove(p);else fluids.put(p,s);worldModel.setFluid(p,s);}
    public VanillaTickEngine ticker(){return ticker;}
    public int sectionCount(){return sections.size();}
    public Map<Integer,int[]> sectionSnapshot(){Map<Integer,int[]> out=new HashMap<>();for(var e:sections.entrySet())out.put(e.getKey(),e.getValue().copyStates());return Map.copyOf(out);}
    public Map<BlockPos,VanillaFluidState> fluidSnapshot(){return Map.copyOf(fluids);}
    public void tick(){if(loaded)ticker.tick();}
}
