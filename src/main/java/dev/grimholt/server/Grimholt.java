package dev.grimholt.server;
import dev.grimholt.server.api.*;
import dev.grimholt.server.config.*;
import dev.grimholt.server.lifecycle.*;
import dev.grimholt.server.logging.Logging;
import dev.grimholt.server.minestom.MinestomAdapter;
import dev.grimholt.server.plugin.PluginBoundary;
import dev.grimholt.server.metrics.MetricsRegistry;
import dev.grimholt.server.security.SecurityLimits;
import dev.grimholt.server.vanilla.VanillaServerKernel;
import java.nio.file.Path;
public final class Grimholt {
 private final Lifecycle lifecycle=new Lifecycle(); private final ConfigLoader configLoader=new ConfigLoader(); private final MinestomAdapter minestom=new MinestomAdapter();
 private final DefaultEventBus events=new DefaultEventBus(); private final DefaultServiceRegistry services=new DefaultServiceRegistry(); private final BoundedScheduler scheduler=new BoundedScheduler(4,1024);
 private final GrimholtServerImpl api=new GrimholtServerImpl(lifecycle,events,scheduler,services); private final MetricsRegistry metrics=new MetricsRegistry(); private final SecurityLimits limits=SecurityLimits.defaults(); private final VanillaServerKernel vanillaKernel=new VanillaServerKernel(1024, task -> scheduler.run(task), failure -> Logging.failure(failure)); private final PluginBoundary plugins=new PluginBoundary(api,scheduler,events,services); private GrimholtConfig config;
 public Grimholt(){services.register(MetricsRegistry.class,metrics);services.register(SecurityLimits.class,limits);api.attachPluginManager(plugins);}
 public static void main(String[] args){Path configPath=args.length==0?Path.of("grimholt.properties"):Path.of(args[0]);Grimholt server=new Grimholt();Runtime.getRuntime().addShutdownHook(new Thread(server::stop,"Grimholt-Shutdown"));server.start(configPath);}
 public synchronized void start(Path configPath){
  if(!lifecycle.beginStart())throw new IllegalStateException("Grimholt cannot start from state "+lifecycle.state());
  try{config=configLoader.load(configPath);Logging.startup(config.bindAddress(),config.port());plugins.start();plugins.discover(Path.of("plugins"));minestom.start(config,api); vanillaKernel.start(); var worldId=minestom.overworldId(); if(worldId!=null) vanillaKernel.registerWorld(worldId); scheduleVanillaTick(); plugins.loadAll();plugins.enableAll();lifecycle.started();events.post(new dev.grimholt.server.event.ServerReadyEvent());Logging.started();}
  catch(Throwable failure){lifecycle.failed();try{plugins.disableAll();}catch(Throwable x){failure.addSuppressed(x);}try{vanillaKernel.stop();}catch(Throwable x){failure.addSuppressed(x);}try{minestom.stop();}catch(Throwable x){failure.addSuppressed(x);}scheduler.close();Logging.failure(failure);throwUnchecked(failure);}
 }
 public synchronized void stop(){
  if(!lifecycle.beginStop()){var s=lifecycle.state();if(s==LifecycleState.STOPPED||s==LifecycleState.NEW)return;if(s==LifecycleState.FAILED){try{plugins.disableAll();}finally{try{minestom.stop();}finally{scheduler.close();}}return;}throw new IllegalStateException("Grimholt cannot stop from state "+s);}
  Logging.stopping();Throwable failure=null;try{plugins.disableAll();}catch(Throwable x){failure=x;}try{vanillaKernel.stop();}catch(Throwable x){if(failure==null)failure=x;else failure.addSuppressed(x);}try{minestom.stop();}catch(Throwable x){if(failure==null)failure=x;else failure.addSuppressed(x);}scheduler.close();lifecycle.stopped();Logging.stopped();if(failure!=null){Logging.failure(failure);throwUnchecked(failure);}
 }
 private static void throwUnchecked(Throwable f){if(f instanceof RuntimeException e)throw e;if(f instanceof Error e)throw e;throw new IllegalStateException("Grimholt lifecycle operation failed",f);}
 public LifecycleState state(){return lifecycle.state();} public GrimholtConfig config(){return config;} public GrimholtServerImpl api(){return api;} public MetricsRegistry.Snapshot metrics(){return metrics.snapshot();} public SecurityLimits limits(){return limits;} public VanillaServerKernel vanillaKernel(){return vanillaKernel;} private void scheduleVanillaTick(){if(!vanillaKernel.running())return; try{scheduler.run(()->{if(vanillaKernel.running())vanillaKernel.tick();scheduleVanillaTick();});}catch(java.util.concurrent.RejectedExecutionException ignored){}}
}