package dev.grimholt.server.vanilla;
import java.util.*;
/** Region-local deterministic redstone propagation with component hooks. */
public final class VanillaRedstoneEngine {
 private final Map<BlockPos,Integer> power=new HashMap<>();
 private final Map<BlockPos,Integer> pulses=new HashMap<>();
 public int power(BlockPos p){return power.getOrDefault(p,0);}
 public void setPower(BlockPos p,int v){if(v<0||v>15)throw new IllegalArgumentException();if(v==0)power.remove(p);else power.put(p,v);}
 public boolean observerPulsing(BlockPos p){return pulses.containsKey(p);}
 public boolean isPowered(VanillaWorldModel w,BlockPos p){
   if(power(p)>0)return true;
   if(power(new BlockPos(p.x(),p.y()+1,p.z()))>0)return true;
   for(BlockPos n:neighbors(p))if(power(n)>0||source(w,n)>0)return true;
   return false;
 }
 public int tick(VanillaWorldModel w,int budget){
   ArrayDeque<BlockPos> q=new ArrayDeque<>(power.keySet());Set<BlockPos> seen=new HashSet<>(q);int n=0;
   while(!q.isEmpty()&&n<budget){
     BlockPos p=q.removeFirst();seen.remove(p);int src=source(w,p);
     for(BlockPos to:neighbors(p)){
       int next=signal(w,p,to,src);
       if(next!=power(to)){setPower(to,next);n++;if(seen.add(to))q.addLast(to);}
     }
     String id=w.getBlock(p).id();
     if(id.equals("minecraft:observer"))pulses.put(p,2);
     if(id.equals("minecraft:piston")||id.equals("minecraft:sticky_piston"))tryExtend(w,p);
   }
   pulses.replaceAll((p,v)->v-1);pulses.entrySet().removeIf(e->e.getValue()<=0);
   return n;
 }
 private int source(VanillaWorldModel w,BlockPos p){
   String id=w.getBlock(p).id();
   if(id.equals("minecraft:redstone_block")||id.equals("minecraft:redstone_torch")||id.equals("minecraft:lever")||id.endsWith("_button"))return 15;
   if(id.equals("minecraft:observer")&&observerPulsing(p))return 15;
   try{return id.equals("minecraft:redstone_wire")?Integer.parseInt(Optional.ofNullable(w.getBlock(p).property("power")).orElse("0")):0;}catch(NumberFormatException e){return 0;}
 }
 private int signal(VanillaWorldModel w,BlockPos from,BlockPos to,int src){
   String id=w.getBlock(to).id();
   if(id.equals("minecraft:redstone_block"))return 15;
   if(id.equals("minecraft:repeater"))return isPowered(w,to)?15:0;
   if(id.equals("minecraft:comparator"))return Math.min(15,maxNeighbors(to));
   if(id.equals("minecraft:observer")&&observerPulsing(to))return 15;
   return Math.max(0,src-1);
 }
 private int maxNeighbors(BlockPos p){int m=0;for(BlockPos n:neighbors(p))m=Math.max(m,power(n));return m;}
 private void tryExtend(VanillaWorldModel w,BlockPos p){
   if(!isPowered(w,p))return;
   String f=Optional.ofNullable(w.getBlock(p).property("facing")).orElse("north");BlockPos head=offset(p,f),push=offset(head,f);
   if(!w.getBlock(head).id().equals("minecraft:air")&&w.getBlock(push).id().equals("minecraft:air")){w.setBlock(push,w.getBlock(head));w.setBlock(head,BlockState.of("minecraft:air"));}
 }
 private static BlockPos offset(BlockPos p,String f){return switch(f){case"up"->new BlockPos(p.x(),p.y()+1,p.z());case"down"->new BlockPos(p.x(),p.y()-1,p.z());case"south"->new BlockPos(p.x(),p.y(),p.z()+1);case"east"->new BlockPos(p.x()+1,p.y(),p.z());case"west"->new BlockPos(p.x()-1,p.y(),p.z());default->new BlockPos(p.x(),p.y(),p.z()-1);};}
 private static List<BlockPos> neighbors(BlockPos p){return List.of(new BlockPos(p.x()+1,p.y(),p.z()),new BlockPos(p.x()-1,p.y(),p.z()),new BlockPos(p.x(),p.y()+1,p.z()),new BlockPos(p.x(),p.y()-1,p.z()),new BlockPos(p.x(),p.y(),p.z()+1),new BlockPos(p.x(),p.y(),p.z()-1));}
}