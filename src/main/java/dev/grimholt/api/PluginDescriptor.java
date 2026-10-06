package dev.grimholt.api;
import java.util.List;
public record PluginDescriptor(String id,String name,String version,String mainClass,String apiVersion,List<String> dependencies,List<String> optionalDependencies) {
 public PluginDescriptor { if(id==null||!id.matches("[a-z0-9][a-z0-9._-]{1,63}")) throw new IllegalArgumentException("Invalid plugin id: "+id); if(name==null||name.isBlank()) throw new IllegalArgumentException("Plugin name is blank"); if(version==null||version.isBlank()) throw new IllegalArgumentException("Plugin version is blank"); if(mainClass==null||mainClass.isBlank()) throw new IllegalArgumentException("Plugin main class is blank"); if(apiVersion==null||apiVersion.isBlank()) throw new IllegalArgumentException("Plugin API version is blank"); dependencies=List.copyOf(dependencies==null?List.of():dependencies); optionalDependencies=List.copyOf(optionalDependencies==null?List.of():optionalDependencies); }
}
