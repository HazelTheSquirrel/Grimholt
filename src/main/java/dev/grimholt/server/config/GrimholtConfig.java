package dev.grimholt.server.config;

import java.net.InetSocketAddress;
import java.util.Objects;

public record GrimholtConfig(int configVersion, String bindAddress, int port) {
    public static final int CURRENT_VERSION = 1;
    public static final String DEFAULT_BIND_ADDRESS = "0.0.0.0";
    public static final int DEFAULT_PORT = 25565;

    public GrimholtConfig {
        Objects.requireNonNull(bindAddress, "bindAddress");
        if (configVersion != CURRENT_VERSION) throw new IllegalArgumentException("Unsupported configuration version: " + configVersion);
        if (bindAddress.isBlank()) throw new IllegalArgumentException("bind-address must not be blank");
        if (port < 0 || port > 65535) throw new IllegalArgumentException("port must be between 0 and 65535");
    }

    public static GrimholtConfig defaults() { return new GrimholtConfig(CURRENT_VERSION, DEFAULT_BIND_ADDRESS, DEFAULT_PORT); }
    public InetSocketAddress socketAddress() { return new InetSocketAddress(bindAddress, port); }
}
