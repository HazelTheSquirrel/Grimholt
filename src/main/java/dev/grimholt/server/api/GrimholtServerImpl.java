package dev.grimholt.server.api;

import dev.grimholt.api.*;
import dev.grimholt.server.command.GrimholtCommandDispatcher;
import dev.grimholt.server.lifecycle.Lifecycle;
import dev.grimholt.server.network.GrimholtConnection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class GrimholtServerImpl implements GrimholtServer {
    private final Lifecycle lifecycle;
    private final DefaultEventBus events;
    private final BoundedScheduler scheduler;
    private final DefaultServiceRegistry services;
    private final GrimholtCommandDispatcher commands;
    private PluginManager plugins;
    private volatile int maxPlayers = 1000;
    private final Map<UUID, GrimholtPlayerImpl> players = new ConcurrentHashMap<>();
    private final Map<UUID, GrimholtWorldImpl> worlds = new ConcurrentHashMap<>();
    private final Map<UUID, GrimholtConnection> connections = new ConcurrentHashMap<>();

    public GrimholtServerImpl(Lifecycle l, DefaultEventBus e, BoundedScheduler s, DefaultServiceRegistry r,
                              GrimholtCommandDispatcher commands) {
        lifecycle = l; events = e; scheduler = s; services = r; this.commands = Objects.requireNonNull(commands);
    }

    public void attachPluginManager(PluginManager p) {
        if (plugins != null) throw new IllegalStateException("Plugin manager already attached");
        plugins = Objects.requireNonNull(p);
    }

    public void configureLimits(int maxPlayers) {
        if (maxPlayers < 1) throw new IllegalArgumentException("maxPlayers");
        this.maxPlayers = maxPlayers;
    }

    public int maxPlayers() { return maxPlayers; }

    public void addWorld(UUID id, String dimension) {
        worlds.putIfAbsent(id, new GrimholtWorldImpl(id, dimension, () -> players.values().stream()
                .map(x -> (GrimholtPlayer) x).toList()));
    }

    public void addPlayer(UUID id, String name, Consumer<String> messageSink, Consumer<String> kickSink) {
        players.put(id, new GrimholtPlayerImpl(id, name, messageSink, kickSink));
    }

    public void playerConnected(UUID id, String name, GrimholtConnection connection) {
        addPlayer(id, name, connection::sendChat, connection::kick);
        connections.put(id, connection);
    }

    public void playerDisconnected(UUID id) {
        connections.remove(id);
        removePlayer(id);
    }

    public void updatePlayer(UUID id, Position position) {
        GrimholtPlayerImpl player = players.get(id);
        if (player != null) player.position(position);
    }

    public void updatePlayerPosition(UUID id, Position position) { updatePlayer(id, position); }

    public void removePlayer(UUID id) {
        players.remove(id);
        connections.remove(id);
    }

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
    public GrimholtCommandDispatcher commandDispatcher() { return commands; }

    @Override public void registerCommand(Command command) { commands.register(command); }

    @Override public void broadcast(String message) {
        players.values().forEach(player -> player.sendMessage(message));
    }

    public void clear() {
        players.clear();
        worlds.clear();
        connections.clear();
        events.clear();
        services.clear();
    }

    public void attachConnection(GrimholtConnection connection) {
        Objects.requireNonNull(connection, "connection");
    }
}
