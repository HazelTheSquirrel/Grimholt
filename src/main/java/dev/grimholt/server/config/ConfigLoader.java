package dev.grimholt.server.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ConfigLoader {
    private static final String CONFIG_VERSION="config-version", BIND_ADDRESS="bind-address", PORT="port", ONLINE_MODE="online-mode",
            MAX_PLAYERS="max-players", DISPATCHER_THREADS="dispatcher-threads", VIEW_DISTANCE="view-distance",
            SIMULATION_DISTANCE="simulation-distance", WORLD_DIRECTORY="world-directory";
    public GrimholtConfig load(Path path) throws IOException {
        if (!Files.exists(path)) { var defaults=GrimholtConfig.defaults(); save(path,defaults); return defaults; }
        Properties p=new Properties(); try(Reader reader=Files.newBufferedReader(path)){p.load(reader);}
        int version=integer(p,CONFIG_VERSION,GrimholtConfig.CURRENT_VERSION);
        String bind=p.getProperty(BIND_ADDRESS,GrimholtConfig.DEFAULT_BIND_ADDRESS).trim();
        int port=integer(p,PORT,GrimholtConfig.DEFAULT_PORT);
        boolean online=bool(p,ONLINE_MODE,GrimholtConfig.DEFAULT_ONLINE_MODE);
        int max=integer(p,MAX_PLAYERS,GrimholtConfig.DEFAULT_MAX_PLAYERS);
        int threads=integer(p,DISPATCHER_THREADS,GrimholtConfig.defaults().dispatcherThreads());
        int view=integer(p,VIEW_DISTANCE,GrimholtConfig.DEFAULT_VIEW_DISTANCE);
        int sim=integer(p,SIMULATION_DISTANCE,GrimholtConfig.DEFAULT_SIMULATION_DISTANCE);
        String world=p.getProperty(WORLD_DIRECTORY,GrimholtConfig.DEFAULT_WORLD_DIRECTORY).trim();
        return new GrimholtConfig(version,bind,port,online,max,threads,view,sim,world);
    }
    public void save(Path path,GrimholtConfig c) throws IOException {
        Path parent=path.toAbsolutePath().getParent(); if(parent!=null) Files.createDirectories(parent);
        Properties p=new Properties(); p.setProperty(CONFIG_VERSION,Integer.toString(c.configVersion())); p.setProperty(BIND_ADDRESS,c.bindAddress());
        p.setProperty(PORT,Integer.toString(c.port())); p.setProperty(ONLINE_MODE,Boolean.toString(c.onlineMode())); p.setProperty(MAX_PLAYERS,Integer.toString(c.maxPlayers()));
        p.setProperty(DISPATCHER_THREADS,Integer.toString(c.dispatcherThreads())); p.setProperty(VIEW_DISTANCE,Integer.toString(c.viewDistance()));
        p.setProperty(SIMULATION_DISTANCE,Integer.toString(c.simulationDistance())); p.setProperty(WORLD_DIRECTORY,c.worldDirectory());
        try(var writer=Files.newBufferedWriter(path)){p.store(writer,"Grimholt server configuration");}
    }
    private static int integer(Properties p,String key,int fallback){String v=p.getProperty(key,Integer.toString(fallback)).trim();try{return Integer.parseInt(v);}catch(NumberFormatException e){throw new IllegalArgumentException("Invalid integer for '"+key+"': "+v,e);}}
    private static boolean bool(Properties p,String key,boolean fallback){String v=p.getProperty(key,Boolean.toString(fallback)).trim();if(!v.equalsIgnoreCase("true")&&!v.equalsIgnoreCase("false"))throw new IllegalArgumentException("Invalid boolean for '"+key+"': "+v);return Boolean.parseBoolean(v);}
}
