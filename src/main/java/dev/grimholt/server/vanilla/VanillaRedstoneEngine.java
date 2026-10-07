package dev.grimholt.server.vanilla;
import java.util.*;
public final class VanillaRedstoneEngine{
 private final Map<BlockPos,Integer> power=new HashMap<>();
 public int power(BlockPos p){return power.getOrDefault(p,0);} public void setPower(BlockPos p,int v){if(v<0||v>15)throw new IllegalArgumentException();power.put(p,v);}
 public int tick(VanillaWorldModel w,int budget){ArrayDeque<BlockPos> q=new ArrayDeque<>(power.keySet());int updates=0;while(!q.isEmpty()&&updates<budget){BlockPos p=q.removeFirst();int source=power(p);for(int[] d:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}}){BlockPos n=new BlockPos(p.x()+d[0],p.y()+d[1],p.z()+d[2]);String id=w.getBlock(n).id();int next=id.equals("minecraft:redstone_block")?15:Math.max(0,source-1);if(next>power(n)){power.put(n,next);q.add(n);}updates++;}}return updates;}
 public Map<BlockPos,Integer> snapshot(){return Map.copyOf(power);}
}