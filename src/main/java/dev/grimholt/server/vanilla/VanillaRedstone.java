package dev.grimholt.server.vanilla;

public final class VanillaRedstone {
    private VanillaRedstone() {}
    public static int power(BlockState state){
        String p=state.property("power"); if(p==null) return state.id().equals("minecraft:redstone_block")?15:0;
        try{return Math.max(0,Math.min(15,Integer.parseInt(p)));}catch(NumberFormatException e){return 0;}
    }
    public static int neighborPower(VanillaChunk chunk,BlockPos pos){int max=0;for(int[]d:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}})max=Math.max(max,power(chunk.block(new BlockPos(pos.x()+d[0],pos.y()+d[1],pos.z()+d[2]))));return max;}
}
