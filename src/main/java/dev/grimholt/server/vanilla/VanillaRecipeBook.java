package dev.grimholt.server.vanilla;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class VanillaRecipeBook {
    private final Map<String,VanillaRecipe> recipes=new ConcurrentHashMap<>();
    public void register(VanillaRecipe recipe){recipes.putIfAbsent(recipe.id(),recipe);}
    public VanillaRecipe get(String id){return recipes.get(id);}
    public Map<String,VanillaRecipe> snapshot(){return Map.copyOf(recipes);}
}
