package dev.grimholt.server.vanilla;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import static org.junit.jupiter.api.Assertions.*;

class VanillaSnapshot26_2Test {
    @Test void targetVersionIsPinned() {
        assertEquals("26.4-snapshot-3", VanillaSnapshot26_2.VERSION);
        assertEquals(1_073_742_165, VanillaSnapshot26_2.PROTOCOL);
        assertEquals(5_122, VanillaSnapshot26_2.WORLD_DATA_VERSION);
        assertEquals("123.0", VanillaSnapshot26_2.DATA_PACK_VERSION);
        assertEquals("100.0", VanillaSnapshot26_2.RESOURCE_PACK_VERSION);
        assertEquals(25, VanillaSnapshot26_2.JAVA_MAJOR);
    }

    @Test void wireFramesRoundTrip() throws Exception {
        VanillaProtocol26_2.Frame frame = new VanillaProtocol26_2.Frame(0x7f, new byte[] {1,2,3,4});
        byte[] encoded = VanillaProtocol26_2.encodeFrame(frame);
        VanillaProtocol26_2.Frame decoded = VanillaProtocol26_2.decodeFrame(new ByteArrayInputStream(encoded), 1024);
        assertEquals(frame.packetId(), decoded.packetId());
        assertArrayEquals(frame.payload(), decoded.payload());
    }

    @Test void generatedManifestIsPinned() {
        VanillaGeneratedData data = new VanillaGeneratedData();
        assertEquals("26.4-snapshot-3",
            data.require("manifest.properties").lines()
                .filter(line -> line.startsWith("version="))
                .findFirst().orElseThrow()
                .substring("version=".length()));
    }
}