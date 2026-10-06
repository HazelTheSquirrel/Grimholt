package dev.grimholt.server.vanilla;

import java.util.List;

public record VanillaRecipe(String id,String type,List<String> ingredients,VanillaItemStack result) {
    public VanillaRecipe { if(id==null||type==null||ingredients==null||result==null)throw new NullPointerException(); ingredients=List.copyOf(ingredients); }
}
