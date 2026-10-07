package dev.grimholt.server.vanilla;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

class VanillaSnapshot26_4S3Test {
    @Test
    void targetVersionIsPinned() {
        assertEquals("26.4-snapshot-3", VanillaSnapshot26_4S3.VERSION);
        assertEquals(1_073_742_165, VanillaSnapshot26_4S3.PROTOCOL);
        assertEquals(5_122, VanillaSnapshot26_4S3.WORLD_DATA_VERSION);
        assertEquals(123, VanillaSnapshot26_4S3.DATA_PACK_VERSION);
    }

    @Test
    void wireFramesRoundTrip() throws Exception {
        VanillaProtocol26_4S3.Frame frame =
            new VanillaProtocol26_4S3.Frame(0x7f, new byte[] {1, 2, 3, 4});
        byte[] encoded = VanillaProtocol26_4S3.encodeFrame(frame);
        VanillaProtocol26_4S3.Frame decoded =
            VanillaProtocol26_4S3.decodeFrame(new ByteArrayInputStream(encoded), 1024);
        assertEquals(frame.packetId(), decoded.packetId());
        assertArrayEquals(frame.payload(), decoded.payload());
    }

    @Test
    void generatedManifestIsPinned() {
        VanillaGeneratedData data = new VanillaGeneratedData();
        assertEquals("26.4-snapshot-3",
            data.require("manifest.properties").lines()
                .filter(line -> line.startsWith("version="))
                .findFirst().orElseThrow()
                .substring("version=".length()));
    }
}
