package dev.grimholt.server.vanilla;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class VanillaScoreboard {
    private final Map<String,Map<String,Integer>> objectives=new ConcurrentHashMap<>();
    public void create(String objective){objectives.putIfAbsent(objective,new ConcurrentHashMap<>());}
    public void set(String objective,String holder,int score){objectives.computeIfAbsent(objective,k->new ConcurrentHashMap<>()).put(holder,score);}
    public int get(String objective,String holder){return objectives.getOrDefault(objective,Map.of()).getOrDefault(holder,0);}
    public void add(String objective,String holder,int delta){set(objective,holder,get(objective,holder)+delta);}
    public Map<String,Integer> snapshot(String objective){return Map.copyOf(objectives.getOrDefault(objective,Map.of()));}
}
