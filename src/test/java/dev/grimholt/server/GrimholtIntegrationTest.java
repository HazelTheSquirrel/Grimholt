package dev.grimholt.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GrimholtIntegrationTest {
    @TempDir Path tempDir;
    @Test void startsAndStopsThroughPublicBootstrap() throws Exception {
        Grimholt server=new Grimholt();
        Path config=tempDir.resolve("grimholt.properties");
        java.nio.file.Files.writeString(config,
                "config-version=2\n"+
                "bind-address=127.0.0.1\n"+
                "port=0\n"+
                "online-mode=false\n"+
                "max-players=1000\n"+
                "dispatcher-threads=2\n"+
                "view-distance=10\n"+
                "simulation-distance=10\n"+
                "world-directory="+tempDir.resolve("world").toString().replace("\\","/")+"\n");
        try { server.start(config); assertEquals(dev.grimholt.server.lifecycle.LifecycleState.RUNNING,server.state()); }
        finally { server.stop(); }
        assertEquals(dev.grimholt.server.lifecycle.LifecycleState.STOPPED,server.state());
    }
}
