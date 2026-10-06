package dev.grimholt.api;
import java.util.List; import java.util.UUID;
public interface GrimholtServer { ServerState state(); List<GrimholtPlayer> players(); List<GrimholtWorld> worlds(); GrimholtPlayer player(UUID uuid); GrimholtWorld world(UUID id); EventBus events(); Scheduler scheduler(); ServiceRegistry services(); PluginManager plugins(); void registerCommand(Command command); void broadcast(String message); }
