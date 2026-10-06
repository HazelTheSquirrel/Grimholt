package dev.grimholt.server.api;
import dev.grimholt.api.*;
import dev.grimholt.server.lifecycle.Lifecycle;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public final class GrimholtServerImpl implements GrimholtServer {
 private final Lifecycle lifecycle; private final DefaultEventBus events; private final BoundedScheduler scheduler; private final DefaultServiceRegistry services; private PluginManager plugins;
 private final Map<UUID,GrimholtPlayer> players=new ConcurrentHashMap<>(); private final Map<UUID,GrimholtWorld> worlds=new ConcurrentHashMap<>();
 public GrimholtServerImpl(Lifecycle l,DefaultEventBus e,BoundedScheduler s,DefaultServiceRegistry r){lifecycle=l;events=e;scheduler=s;services=r;}
 public void attachPluginManager(PluginManager p){if(plugins!=null)throw new IllegalStateException("Plugin manager already attached");plugins=Objects.requireNonNull(p);}
 public void addWorld(net.minestom.server.instance.Instance i){worlds.put(i.getUuid(),new MinestomWorld(i));}
 public void addPlayer(net.minestom.server.entity.Player p){players.put(p.getUuid(),new MinestomPlayer(p));}
 public void removePlayer(UUID id){players.remove(id);}
 public ServerState state(){return switch(lifecycle.state()){case NEW->ServerState.NEW;case STARTING->ServerState.STARTING;case RUNNING->ServerState.RUNNING;case STOPPING->ServerState.STOPPING;case STOPPED->ServerState.STOPPED;case FAILED->ServerState.FAILED;};}
 public List<GrimholtPlayer> players(){return List.copyOf(players.values());} public List<GrimholtWorld> worlds(){return List.copyOf(worlds.values());}
 public GrimholtPlayer player(UUID id){return players.get(id);} public GrimholtWorld world(UUID id){return worlds.get(id);}
 public EventBus events(){return events;} public Scheduler scheduler(){return scheduler;} public ServiceRegistry services(){return services;}
 public PluginManager plugins(){return Objects.requireNonNull(plugins,"Plugin manager not attached");}
 public void registerCommand(dev.grimholt.api.Command c){
  Objects.requireNonNull(c); Objects.requireNonNull(c.name());
  var n=new Command(c.name(),c.aliases()==null?new String[0]:c.aliases().toArray(String[]::new));
  n.setDefaultExecutor((sender,context)->{
   String raw=context.getInput().trim(); String[] parts=raw.isEmpty()?new String[0]:raw.split("\\\\s+");
   String[] args=parts.length<=1?new String[0]:java.util.Arrays.copyOfRange(parts,1,parts.length);
   c.execute(new MinestomCommandSender(sender),args);
  });
  MinecraftServer.getCommandManager().register(n);
 }
 public void broadcast(String m){players.values().forEach(p->p.sendMessage(m));} public void clear(){players.clear();worlds.clear();events.clear();services.clear();}
}