package dev.grimholt.server.vanilla;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class VanillaDataPack {
    private final Map<String,String> resources=new ConcurrentHashMap<>();
    public void put(String key,String value){resources.put(key,value);}
    public String get(String key){return resources.get(key);}
    public Map<String,String> snapshot(){return Map.copyOf(resources);}
}
