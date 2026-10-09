package dev.grimholt.server.vanilla;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class VanillaSnapshot26_2Test {
    @Test void targetVersionIsPinnedInNeutralContract() {
        assertEquals("26.4-snapshot-3", VanillaSnapshot.VERSION);
        assertEquals("26.4-snapshot-3", VanillaSnapshot.MOJANG_VERSION_ID);
        assertEquals(1_073_742_165, VanillaSnapshot.PROTOCOL);
        assertEquals(5_122, VanillaSnapshot.WORLD_DATA_VERSION);
        assertEquals("123.0", VanillaSnapshot.DATA_PACK_VERSION);
        assertEquals("100.0", VanillaSnapshot.RESOURCE_PACK_VERSION);
        assertEquals(25, VanillaSnapshot.JAVA_MAJOR);
        assertEquals("2d89c95c030e635387448f332961074ce1adbb4b", VanillaSnapshot.SERVER_SHA1);
    }

    @Test void legacySnapshotAliasCannotDrift() {
        assertEquals(VanillaSnapshot.VERSION, VanillaSnapshot26_2.VERSION);
        assertEquals(VanillaSnapshot.PROTOCOL, VanillaSnapshot26_2.PROTOCOL);
        assertEquals(VanillaSnapshot.WORLD_DATA_VERSION, VanillaSnapshot26_2.WORLD_DATA_VERSION);
        assertEquals(VanillaSnapshot.DATA_PACK_VERSION, VanillaSnapshot26_2.DATA_PACK_VERSION);
        assertEquals(VanillaSnapshot.RESOURCE_PACK_VERSION, VanillaSnapshot26_2.RESOURCE_PACK_VERSION);
        assertEquals(VanillaSnapshot.JAVA_MAJOR, VanillaSnapshot26_2.JAVA_MAJOR);
        assertEquals(VanillaSnapshot.SERVER_SHA1, VanillaSnapshot26_2.SERVER_SHA1);
    }

    @Test void wireFramesRoundTrip() throws Exception {
        VanillaProtocol26_2.Frame frame = new VanillaProtocol26_2.Frame(0x7f, new byte[] {1,2,3,4});
        byte[] encoded = VanillaProtocol26_2.encodeFrame(frame);
        VanillaProtocol26_2.Frame decoded = VanillaProtocol26_2.decodeFrame(new ByteArrayInputStream(encoded), 1024);
        assertEquals(frame.packetId(), decoded.packetId());
        assertArrayEquals(frame.payload(), decoded.payload());
    }

    @Test void generatedManifestMatchesPinnedContract() throws Exception {
        Properties manifest = new Properties();
        manifest.load(new ByteArrayInputStream(new VanillaGeneratedData().require("manifest.properties").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertEquals(VanillaSnapshot.VERSION, manifest.getProperty("version"));
        assertEquals(Integer.toString(VanillaSnapshot.PROTOCOL), manifest.getProperty("protocol"));
        assertEquals(Integer.toString(VanillaSnapshot.WORLD_DATA_VERSION), manifest.getProperty("worldDataVersion"));
        assertEquals(VanillaSnapshot.DATA_PACK_VERSION, manifest.getProperty("dataPackVersion"));
        assertEquals(VanillaSnapshot.RESOURCE_PACK_VERSION, manifest.getProperty("resourcePackVersion"));
        assertEquals(Integer.toString(VanillaSnapshot.JAVA_MAJOR), manifest.getProperty("javaMajor"));
        assertEquals(VanillaSnapshot.SERVER_SHA1, manifest.getProperty("serverSha1"));
        assertEquals("reference/minecraft/26.4/server.jar", manifest.getProperty("serverPath"));
    }
}
