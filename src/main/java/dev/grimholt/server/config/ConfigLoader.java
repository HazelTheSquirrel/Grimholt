package dev.grimholt.server.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ConfigLoader {
    private static final String CONFIG_VERSION = "config-version";
    private static final String BIND_ADDRESS = "bind-address";
    private static final String PORT = "port";

    public GrimholtConfig load(Path path) throws IOException {
        if (!Files.exists(path)) {
            GrimholtConfig defaults = GrimholtConfig.defaults();
            save(path, defaults);
            return defaults;
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) { properties.load(reader); }
        int version = integer(properties, CONFIG_VERSION, GrimholtConfig.CURRENT_VERSION);
        String bindAddress = properties.getProperty(BIND_ADDRESS, GrimholtConfig.DEFAULT_BIND_ADDRESS).trim();
        int port = integer(properties, PORT, GrimholtConfig.DEFAULT_PORT);
        return new GrimholtConfig(version, bindAddress, port);
    }

    public void save(Path path, GrimholtConfig config) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Properties properties = new Properties();
        properties.setProperty(CONFIG_VERSION, Integer.toString(config.configVersion()));
        properties.setProperty(BIND_ADDRESS, config.bindAddress());
        properties.setProperty(PORT, Integer.toString(config.port()));
        try (var writer = Files.newBufferedWriter(path)) { properties.store(writer, "Grimholt server configuration"); }
    }

    private static int integer(Properties properties, String key, int fallback) {
        String value = properties.getProperty(key, Integer.toString(fallback)).trim();
        try { return Integer.parseInt(value); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Invalid integer for '" + key + "': " + value, exception); }
    }
}
