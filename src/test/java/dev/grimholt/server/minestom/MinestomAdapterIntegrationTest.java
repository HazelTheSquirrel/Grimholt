package dev.grimholt.server.minestom;

import dev.grimholt.server.config.GrimholtConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MinestomAdapterIntegrationTest {
    @Test void startsAndStopsOnEphemeralPort() {
        MinestomAdapter adapter=new MinestomAdapter();
        GrimholtConfig config=new GrimholtConfig(2,"127.0.0.1",0,false,1000,2,10,10,"build/test-world");
        try { adapter.start(config); assertTrue(adapter.isStarted()); }
        finally { adapter.stop(); }
        assertFalse(adapter.isStarted());
    }
}
