package dev.grimholt.server.vanilla;

import java.util.ArrayList;
import java.util.List;

public final class VanillaFluidEngine {
    public List<BlockPos> spread(VanillaChunk chunk, BlockPos source) {
        VanillaFluidState state=chunk.fluid(source);
        if(state.isEmpty()) return List.of();
        List<BlockPos> targets=new ArrayList<>();
        int next=state.source()?1:Math.min(8,state.level()+1);
        if(next>8) return List.of();
        for(int[] d:new int[][]{{1,0,0},{-1,0,0},{0,0,1},{0,0,-1},{0,-1,0}}){
            BlockPos p=new BlockPos(source.x()+d[0],source.y()+d[1],source.z()+d[2]);
            if(chunk.block(p).id().equals("minecraft:air") && chunk.fluid(p).isEmpty()) targets.add(p);
        }
        return targets;
    }
    public void tick(VanillaChunk chunk, BlockPos source){
        VanillaFluidState state=chunk.fluid(source); if(state.isEmpty()) return;
        for(BlockPos target:spread(chunk,source)) chunk.setFluid(target,new VanillaFluidState(state.id(),state.source()?1:Math.min(8,state.level()+1),false));
    }
}
