package dev.grimholt.server.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {
    @TempDir Path tempDir;
    @Test void createsAndReloadsDefaults() throws Exception {
        Path file = tempDir.resolve("grimholt.properties");
        ConfigLoader loader = new ConfigLoader();
        GrimholtConfig first = loader.load(file);
        GrimholtConfig second = loader.load(file);
        assertEquals(GrimholtConfig.defaults(), first);
        assertEquals(first, second);
    }
    @Test void rejectsMalformedInteger() throws Exception {
        Path file = tempDir.resolve("grimholt.properties");
        java.nio.file.Files.writeString(file, "port=not-a-number\n");
        assertThrows(IllegalArgumentException.class, () -> new ConfigLoader().load(file));
    }
    @Test void rejectsInvalidPort() throws Exception {
        Path file = tempDir.resolve("grimholt.properties");
        java.nio.file.Files.writeString(file, "port=70000\n");
        assertThrows(IllegalArgumentException.class, () -> new ConfigLoader().load(file));
    }
}
