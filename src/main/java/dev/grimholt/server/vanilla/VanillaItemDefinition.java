package dev.grimholt.server.vanilla;
import java.util.Map;
public record VanillaItemDefinition(String id,int maxStackSize,int maxDamage,boolean edible,Map<String,String> defaultComponents){
 public VanillaItemDefinition{if(id==null||maxStackSize<1||maxStackSize>99||maxDamage<0)throw new IllegalArgumentException();defaultComponents=Map.copyOf(defaultComponents==null?Map.of():defaultComponents);}
}