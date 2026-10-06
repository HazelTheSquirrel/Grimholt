package dev.grimholt.server.plugin;
import dev.grimholt.api.*; import java.nio.file.Path; import java.util.List;
public final class PluginBoundary implements PluginManager {
 private final GrimholtServer server; private final Scheduler scheduler; private final EventBus events; private final ServiceRegistry services; private PluginLoader loader; private boolean started;
 public PluginBoundary(GrimholtServer s,Scheduler sch,EventBus e,ServiceRegistry sv){server=s;scheduler=sch;events=e;services=sv;}
 public void start(){if(started)throw new IllegalStateException("Plugin boundary already started");started=true;loader=new PluginLoader(server,scheduler,events,services);}
 public void discover(Path d){require();loader.discover(d);} public void loadAll(){require();loader.loadAll();} public void enableAll(){require();loader.enableAll();}
 public void disableAll(){if(!started)return;loader.disableAll();started=false;} public List<PluginDescriptor> plugins(){return loader==null?List.of():loader.descriptors();}
 private void require(){if(!started)throw new IllegalStateException("Plugin boundary is not started");}
}