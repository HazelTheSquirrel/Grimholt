package dev.grimholt.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GrimholtIntegrationTest {
    @TempDir Path tempDir;
    @Test void startsAndStopsThroughPublicBootstrap() {
        Grimholt server=new Grimholt(); Path config=tempDir.resolve("grimholt.properties");
        try { server.start(config); assertEquals(dev.grimholt.server.lifecycle.LifecycleState.RUNNING,server.state()); }
        finally { server.stop(); }
        assertEquals(dev.grimholt.server.lifecycle.LifecycleState.STOPPED,server.state());
    }
}
