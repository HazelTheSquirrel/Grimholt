package dev.grimholt.server.config;

import java.net.InetSocketAddress;
import java.util.Objects;

public record GrimholtConfig(int configVersion, String bindAddress, int port, boolean onlineMode, int maxPlayers,
                              int dispatcherThreads, int viewDistance, int simulationDistance, String worldDirectory) {
    public static final int CURRENT_VERSION = 2;
    public static final String DEFAULT_BIND_ADDRESS = "0.0.0.0";
    public static final int DEFAULT_PORT = 25565;
    public static final boolean DEFAULT_ONLINE_MODE = true;
    public static final int DEFAULT_MAX_PLAYERS = 1000;
    public static final int DEFAULT_VIEW_DISTANCE = 10;
    public static final int DEFAULT_SIMULATION_DISTANCE = 10;
    public static final String DEFAULT_WORLD_DIRECTORY = "world";

    public GrimholtConfig {
        Objects.requireNonNull(bindAddress, "bindAddress"); Objects.requireNonNull(worldDirectory, "worldDirectory");
        if (configVersion != CURRENT_VERSION) throw new IllegalArgumentException("Unsupported configuration version: " + configVersion);
        if (bindAddress.isBlank()) throw new IllegalArgumentException("bind-address must not be blank");
        if (worldDirectory.isBlank()) throw new IllegalArgumentException("world-directory must not be blank");
        if (port < 0 || port > 65535) throw new IllegalArgumentException("port must be between 0 and 65535");
        if (maxPlayers < 1 || maxPlayers > 10000) throw new IllegalArgumentException("max-players must be between 1 and 10000");
        if (dispatcherThreads < 1 || dispatcherThreads > 256) throw new IllegalArgumentException("dispatcher-threads must be between 1 and 256");
        if (viewDistance < 2 || viewDistance > 32) throw new IllegalArgumentException("view-distance must be between 2 and 32");
        if (simulationDistance < 2 || simulationDistance > 32) throw new IllegalArgumentException("simulation-distance must be between 2 and 32");
        if (simulationDistance > viewDistance) throw new IllegalArgumentException("simulation-distance must not exceed view-distance");
    }
    public static GrimholtConfig defaults() {
        int threads = Math.min(32, Math.max(2, Runtime.getRuntime().availableProcessors() - 1));
        return new GrimholtConfig(CURRENT_VERSION, DEFAULT_BIND_ADDRESS, DEFAULT_PORT, DEFAULT_ONLINE_MODE,
                DEFAULT_MAX_PLAYERS, threads, DEFAULT_VIEW_DISTANCE, DEFAULT_SIMULATION_DISTANCE, DEFAULT_WORLD_DIRECTORY);
    }
    public InetSocketAddress socketAddress() { return new InetSocketAddress(bindAddress, port); }
}
