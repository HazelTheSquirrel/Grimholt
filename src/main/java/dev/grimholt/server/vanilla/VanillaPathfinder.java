package dev.grimholt.server.vanilla;

import java.util.*;

public final class VanillaPathfinder {
    private record Node(BlockPos p, Node prev, int g, int f) {}
    public List<BlockPos> find(VanillaWorldModel world, BlockPos start, BlockPos goal, int maxNodes) {
        PriorityQueue<Node> open=new PriorityQueue<>(Comparator.comparingInt(Node::f));
        Map<BlockPos,Integer> best=new HashMap<>(); open.add(new Node(start,null,0,0)); best.put(start,0);
        int visited=0;
        while(!open.isEmpty()&&visited++<maxNodes){
            Node n=open.poll(); if(n.p.equals(goal))return build(n);
            for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
                BlockPos p=new BlockPos(n.p.x()+d[0],n.p.y(),n.p.z()+d[1]);
                if(!walkable(world,p))continue; int g=n.g+1;
                if(g>=best.getOrDefault(p,Integer.MAX_VALUE))continue;
                best.put(p,g);open.add(new Node(p,n,g,g+Math.abs(goal.x()-p.x())+Math.abs(goal.z()-p.z())));
            }
        }
        return List.of();
    }
    private boolean walkable(VanillaWorldModel w,BlockPos p){VanillaBlockDefinition d=w.blockRegistry().definition(w.getBlock(p).id());return d==null||!d.solid();}
    private List<BlockPos> build(Node n){List<BlockPos> out=new ArrayList<>();for(Node x=n;x!=null;x=x.prev)out.add(x.p);Collections.reverse(out);return out;}
}
