package dev.grimholt.server.plugin;

import dev.grimholt.api.*;
import dev.grimholt.server.logging.Slf4jGrimholtLogger;
import org.slf4j.*;
import java.io.*; import java.net.*; import java.nio.file.*; import java.util.*; import java.util.jar.JarFile;

final class PluginLoader {
    static final String DESCRIPTOR="grimholt-plugin.properties";
    private final GrimholtServer server; private final Scheduler scheduler; private final EventBus events; private final ServiceRegistry services;
    private final Logger logger=LoggerFactory.getLogger("Grimholt-Plugins"); private final Map<String,Loaded> loaded=new LinkedHashMap<>(); private final List<Path> jars=new ArrayList<>();
    PluginLoader(GrimholtServer s,Scheduler sch,EventBus e,ServiceRegistry sv){server=s;scheduler=sch;events=e;services=sv;}
    void discover(Path dir){jars.clear();if(!Files.exists(dir))return;try(var stream=Files.list(dir)){stream.filter(p->p.getFileName().toString().endsWith(".jar")).sorted().forEach(jars::add);}catch(IOException e){throw new UncheckedIOException("Cannot scan plugin directory "+dir,e);}}
    void loadAll(){var candidates=new LinkedHashMap<String,Candidate>();for(var jar:jars){try(var jf=new JarFile(jar.toFile())){var en=jf.getJarEntry(DESCRIPTOR);if(en==null)throw new IllegalArgumentException("Missing "+DESCRIPTOR+" in "+jar);var p=new Properties();try(var in=jf.getInputStream(en)){p.load(in);}var d=parse(p);if(candidates.putIfAbsent(d.id(),new Candidate(jar,d))!=null)throw new IllegalArgumentException("Duplicate plugin id: "+d.id());}catch(IOException e){throw new UncheckedIOException("Cannot read plugin "+jar,e);}}var order=resolve(candidates);for(var c:order)load(c);for(var l:new ArrayList<>(loaded.values()))try{l.plugin.onLoad(l.context);}catch(Throwable t){disableOne(l);throw new IllegalStateException("Plugin load failed: "+l.descriptor.id(),t);}}
    private PluginDescriptor parse(Properties p){return new PluginDescriptor(req(p,"id"),req(p,"name"),req(p,"version"),req(p,"main-class"),req(p,"api-version"),csv(p.getProperty("depends")),csv(p.getProperty("soft-depends")));}
    private static String req(Properties p,String k){var v=p.getProperty(k);if(v==null||v.isBlank())throw new IllegalArgumentException("Missing plugin property: "+k);return v.trim();}
    private static List<String> csv(String v){if(v==null||v.isBlank())return List.of();return Arrays.stream(v.split(",")).map(String::trim).filter(x->!x.isBlank()).distinct().toList();}
    private List<Candidate> resolve(Map<String,Candidate> all){var out=new ArrayList<Candidate>();var visiting=new HashSet<String>();var visited=new HashSet<String>();for(var c:all.values())visit(c,all,visiting,visited,out);return out;}
    private void visit(Candidate c,Map<String,Candidate> all,Set<String> visiting,Set<String> visited,List<Candidate> out){var id=c.descriptor.id();if(visited.contains(id))return;if(!visiting.add(id))throw new IllegalArgumentException("Plugin dependency cycle at "+id);for(var dep:c.descriptor.dependencies()){var x=all.get(dep);if(x==null)throw new IllegalArgumentException("Missing plugin dependency "+dep+" for "+id);visit(x,all,visiting,visited,out);}for(var dep:c.descriptor.optionalDependencies()){var x=all.get(dep);if(x!=null)visit(x,all,visiting,visited,out);}visiting.remove(id);visited.add(id);out.add(c);}
    private void load(Candidate c){try{var cl=new URLClassLoader(new URL[]{c.jar.toUri().toURL()},PluginLoader.class.getClassLoader());var type=Class.forName(c.descriptor.mainClass(),true,cl);var plugin=(GrimholtPlugin)type.getDeclaredConstructor().newInstance();var context=new Context(c.descriptor,server,scheduler,events,services,new Slf4jGrimholtLogger(LoggerFactory.getLogger("Grimholt-Plugin."+c.descriptor.id())));loaded.put(c.descriptor.id(),new Loaded(c.descriptor,plugin,cl,context));}catch(ReflectiveOperationException|IOException e){throw new IllegalStateException("Cannot load plugin "+c.descriptor.id(),e);}}
    void enableAll(){for(var l:new ArrayList<>(loaded.values()))try{l.plugin.onEnable();}catch(Throwable t){disableOne(l);throw new IllegalStateException("Plugin enable failed: "+l.descriptor.id(),t);}}
    void disableAll(){var list=new ArrayList<>(loaded.values());Collections.reverse(list);for(var l:list)disableOne(l);loaded.clear();}
    private void disableOne(Loaded l){try{l.plugin.onDisable();}catch(Throwable t){logger.error("Plugin disable failed: {}",l.descriptor.id(),t);}try{l.classLoader.close();}catch(IOException e){logger.error("Plugin classloader close failed: {}",l.descriptor.id(),e);}loaded.remove(l.descriptor.id());}
    List<PluginDescriptor> descriptors(){return loaded.values().stream().map(Loaded::descriptor).toList();}
    private record Candidate(Path jar,PluginDescriptor descriptor){} private record Loaded(PluginDescriptor descriptor,GrimholtPlugin plugin,URLClassLoader classLoader,Context context){}
    private record Context(PluginDescriptor descriptor,GrimholtServer server,Scheduler scheduler,EventBus events,ServiceRegistry services,GrimholtLogger logger) implements GrimholtPluginContext{}
}
