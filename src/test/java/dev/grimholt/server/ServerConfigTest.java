package dev.grimholt.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServerConfigTest {
    @Test
    void acceptsValidConfiguration() {
        assertEquals(new ServerConfig("127.0.0.1", 25566),
                new ServerConfig("127.0.0.1", 25566));
    }

    @Test
    void rejectsBlankHost() {
        assertThrows(IllegalArgumentException.class, () -> new ServerConfig("  ", 25565));
    }

    @Test
    void rejectsInvalidPort() {
        assertThrows(IllegalArgumentException.class, () -> new ServerConfig("localhost", 65_536));
        assertThrows(IllegalArgumentException.class, () -> new ServerConfig("localhost", 0));
    }
}
