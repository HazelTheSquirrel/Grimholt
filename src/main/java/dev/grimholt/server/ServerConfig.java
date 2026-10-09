package dev.grimholt.server;

/**
 * Immutable process configuration. System properties take precedence over environment variables.
 */
public record ServerConfig(String host, int port) {
    private static final String DEFAULT_HOST = "0.0.0.0";
    private static final int DEFAULT_PORT = 25565;

    public ServerConfig {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Bind host must not be blank");
        }
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535: " + port);
        }
    }

    public static ServerConfig load() {
        final String host = value("grimholt.host", "GRIMHOLT_HOST", DEFAULT_HOST);
        final String rawPort = value("grimholt.port", "GRIMHOLT_PORT", Integer.toString(DEFAULT_PORT));
        final int port;
        try {
            port = Integer.parseInt(rawPort);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid port value: " + rawPort, exception);
        }
        return new ServerConfig(host, port);
    }

    private static String value(String property, String environment, String fallback) {
        final String systemValue = System.getProperty(property);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue.trim();
        }
        final String environmentValue = System.getenv(environment);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue.trim();
        }
        return fallback;
    }
}
