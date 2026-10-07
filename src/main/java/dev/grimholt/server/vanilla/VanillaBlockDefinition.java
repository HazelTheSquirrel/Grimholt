package dev.grimholt.server.vanilla;
import java.util.Map;
public record VanillaBlockDefinition(String id,float hardness,boolean solid,boolean opaque,boolean randomTick,boolean redstoneConductor,boolean fluidReplaceable,Map<String,String> defaultProperties){
 public VanillaBlockDefinition{if(id==null||hardness<0)throw new IllegalArgumentException();defaultProperties=Map.copyOf(defaultProperties==null?Map.of():defaultProperties);}
 public BlockState defaultState(){return new BlockState(id,defaultProperties);}
}