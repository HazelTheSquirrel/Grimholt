package dev.grimholt.server.minestom;
import dev.grimholt.server.api.GrimholtServerImpl;
import dev.grimholt.server.api.MinestomPlayer;
import dev.grimholt.server.event.*;
import dev.grimholt.server.config.GrimholtConfig;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.world.DimensionType;
public final class MinestomAdapter {
 private MinecraftServer server; private InstanceContainer overworld;
 public void start(GrimholtConfig config){
  if(server!=null)throw new IllegalStateException("Minestom adapter already initialized");
  MinecraftServer initialized=MinecraftServer.init(new Auth.Offline()); server=initialized;
  try{initialized.start(config.socketAddress());}catch(RuntimeException|Error failure){server=null;try{MinecraftServer.stopCleanly();}catch(RuntimeException|Error cleanup){failure.addSuppressed(cleanup);}throw failure;}
 }

 public void start(GrimholtConfig config,GrimholtServerImpl api){
  if(server!=null)throw new IllegalStateException("Minestom adapter already initialized");
  MinecraftServer initialized=MinecraftServer.init(new Auth.Offline()); server=initialized;
  try{
   overworld=MinecraftServer.getInstanceManager().createInstanceContainer(DimensionType.OVERWORLD);
   overworld.enableAutoChunkLoad(true); api.addWorld(overworld);
   var events=MinecraftServer.getGlobalEventHandler();
   events.addListener(AsyncPlayerConfigurationEvent.class,e->{e.setSpawningInstance(overworld);e.getPlayer().setRespawnPoint(new Pos(0,64,0));});
   events.addListener(PlayerSpawnEvent.class,e->{if(e.isFirstSpawn()){var p=new MinestomPlayer(e.getPlayer());api.addPlayer(e.getPlayer());api.events().post(new PlayerJoinEvent(p));}});
   events.addListener(PlayerDisconnectEvent.class,e->{var p=new MinestomPlayer(e.getPlayer());api.events().post(new PlayerQuitEvent(p));api.removePlayer(e.getPlayer().getUuid());});
   initialized.start(config.socketAddress());
  }catch(RuntimeException|Error failure){overworld=null;server=null;try{MinecraftServer.stopCleanly();}catch(RuntimeException|Error cleanup){failure.addSuppressed(cleanup);}throw failure;}
 }
 public void stop(){if(server==null)return;try{if(overworld!=null)overworld.saveChunksToStorage().join();MinecraftServer.stopCleanly();}finally{overworld=null;server=null;}}
 public boolean isStarted(){return server!=null&&MinecraftServer.isStarted();}
}