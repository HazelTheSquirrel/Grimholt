package dev.grimholt.server.vanilla;
import java.util.*;import java.util.concurrent.ConcurrentHashMap;
public final class VanillaPersistenceModel{
 public static final int FORMAT_VERSION=1;
 public record PlayerData(UUID uuid,VanillaPlayerState.GameMode gameMode,float health,float hunger,float saturation,int experience,int level,Map<Integer,VanillaItemStack> inventory){}
 public record EntityData(UUID uuid,String typeId,double x,double y,double z,float health){}
 private final Map<UUID,PlayerData>players=new ConcurrentHashMap<>();private final Map<UUID,EntityData>entities=new ConcurrentHashMap<>();
 public void savePlayer(VanillaPlayerState p){Map<Integer,VanillaItemStack>inv=new HashMap<>();for(int i=0;i<41;i++){var s=p.inventory().get(i);if(!s.isEmpty())inv.put(i,s);}players.put(p.uuid(),new PlayerData(p.uuid(),p.gameMode(),p.health(),p.hunger(),p.saturation(),p.experience(),p.level(),Map.copyOf(inv)));}
 public PlayerData player(UUID id){return players.get(id);}public void saveEntity(VanillaEntityState e){entities.put(e.uuid(),new EntityData(e.uuid(),e.typeId(),e.x(),e.y(),e.z(),e.health()));}public EntityData entity(UUID id){return entities.get(id);}public Map<UUID,PlayerData>playerSnapshot(){return Map.copyOf(players);}public Map<UUID,EntityData>entitySnapshot(){return Map.copyOf(entities);}
}