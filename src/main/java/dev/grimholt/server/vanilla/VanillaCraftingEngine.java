package dev.grimholt.server.vanilla;
import java.util.*;
public final class VanillaCraftingEngine{
 public record Recipe(String id,int width,int height,List<VanillaItemStack> ingredients,VanillaItemStack result){}
 private final List<Recipe> recipes=new ArrayList<>();
 public void register(Recipe r){recipes.add(Objects.requireNonNull(r));} public List<Recipe> recipes(){return List.copyOf(recipes);}
 public VanillaItemStack craft(List<VanillaItemStack> input){for(Recipe r:recipes)if(count(r.ingredients()).equals(count(input)))return r.result();return VanillaItemStack.empty();}
 private Map<String,Integer> count(List<VanillaItemStack> x){Map<String,Integer> m=new HashMap<>();for(var s:x)if(!s.isEmpty())m.merge(s.itemId(),s.count(),Integer::sum);return m;}
 public void registerDefaults(){register(new Recipe("minecraft:planks_from_log",1,1,List.of(new VanillaItemStack("minecraft:oak_log",1,64,Map.of())),new VanillaItemStack("minecraft:oak_planks",4,64,Map.of())));register(new Recipe("minecraft:bread",3,1,List.of(new VanillaItemStack("minecraft:wheat",1,64,Map.of()),new VanillaItemStack("minecraft:wheat",1,64,Map.of()),new VanillaItemStack("minecraft:wheat",1,64,Map.of())),new VanillaItemStack("minecraft:bread",1,64,Map.of())));}
}