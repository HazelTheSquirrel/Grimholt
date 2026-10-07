package dev.grimholt.server.api;

import dev.grimholt.api.*;
import dev.grimholt.server.lifecycle.Lifecycle;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class GrimholtServerImpl implements GrimholtServer {
    private final Lifecycle lifecycle;
    private final DefaultEventBus events;
    private final BoundedScheduler scheduler;
    private final DefaultServiceRegistry services;
    private PluginManager plugins;
    private final Map<UUID, GrimholtPlayerImpl> players = new ConcurrentHashMap<>();
    private final Map<UUID, GrimholtWorldImpl> worlds = new ConcurrentHashMap<>();

    public GrimholtServerImpl(Lifecycle l, DefaultEventBus e, BoundedScheduler s, DefaultServiceRegistry r) {
        lifecycle = l; events = e; scheduler = s; services = r;
    }

    public void attachPluginManager(PluginManager p) {
        if (plugins != null) throw new IllegalStateException("Plugin manager already attached");
        plugins = Objects.requireNonNull(p);
    }

    public void addWorld(UUID id, String dimension) {
        worlds.put(id, new GrimholtWorldImpl(id, dimension, () -> players.values().stream()
                .map(x -> (GrimholtPlayer) x).toList()));
    }

    public void addPlayer(UUID id, String name, Consumer<String> messageSink, Consumer<String> kickSink) {
        players.put(id, new GrimholtPlayerImpl(id, name, messageSink, kickSink));
    }

    public void updatePlayer(UUID id, Position position) {
        GrimholtPlayerImpl player = players.get(id);
        if (player != null) player.position(position);
    }

    public void removePlayer(UUID id) { players.remove(id); }

    public ServerState state() {
        return switch (lifecycle.state()) {
            case NEW -> ServerState.NEW;
            case STARTING -> ServerState.STARTING;
            case RUNNING -> ServerState.RUNNING;
            case STOPPING -> ServerState.STOPPING;
            case STOPPED -> ServerState.STOPPED;
            case FAILED -> ServerState.FAILED;
        };
    }

    public List<GrimholtPlayer> players() { return List.copyOf(players.values()); }
    public List<GrimholtWorld> worlds() { return List.copyOf(worlds.values()); }
    public GrimholtPlayer player(UUID id) { return players.get(id); }
    public GrimholtWorld world(UUID id) { return worlds.get(id); }
    public EventBus events() { return events; }
    public Scheduler scheduler() { return scheduler; }
    public ServiceRegistry services() { return services; }
    public PluginManager plugins() { return Objects.requireNonNull(plugins, "Plugin manager not attached"); }
    public void bindTickScheduler(java.util.function.Consumer<Runnable> executor) { scheduler.bindTickExecutor(executor); }

    public void registerCommand(dev.grimholt.api.Command command) {
        throw new UnsupportedOperationException(
                "Command transport is not yet owned by the Grimholt runtime");
    }

    public void broadcast(String message) {
        players.values().forEach(player -> player.sendMessage(message));
    }

    public void clear() {
        players.clear();
        worlds.clear();
        events.clear();
        services.clear();
    }
}
