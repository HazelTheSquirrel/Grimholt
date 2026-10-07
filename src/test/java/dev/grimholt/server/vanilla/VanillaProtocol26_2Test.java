package dev.grimholt.server.vanilla;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VanillaProtocol26_2Test {
    @Test
    void frameRoundTrips() throws Exception {
        var frame = new VanillaProtocol26_2.Frame(17, new byte[]{1, 2, 3});
        byte[] encoded = VanillaProtocol26_2.encodeFrame(frame);
        assertEquals(frame, VanillaProtocol26_2.decodeFrame(new ByteArrayInputStream(encoded), 1024));
    }

    @Test
    void wirePrimitivesRoundTrip() throws Exception {
        var out = new ByteArrayOutputStream();
        VanillaProtocolCodec.writeString(out, "Grimholt", 32);
        UUID uuid = UUID.randomUUID();
        VanillaProtocolCodec.writeUuid(out, uuid);
        VanillaProtocolCodec.writeByteArray(out, new byte[]{9, 8, 7}, 16);

        var in = new ByteArrayInputStream(out.toByteArray());
        assertEquals("Grimholt", VanillaProtocolCodec.readString(in, 32));
        assertEquals(uuid, VanillaProtocolCodec.readUuid(in));
        assertArrayEquals(new byte[]{9, 8, 7}, VanillaProtocolCodec.readByteArray(in, 16));
    }

    @Test
    void packetCatalogReadsMojangReportShape() throws Exception {
        Path root = Files.createTempDirectory("grimholt-packets");
        Files.createDirectories(root.resolve("reports"));
        Files.writeString(root.resolve("reports/packets.json"), """
            {
              "play": {
                "clientbound": {
                  "minecraft:disconnect": { "protocol_id": 26 }
                },
                "serverbound": {
                  "minecraft:chat": { "protocol_id": 12 }
                }
              },
              "handshake": {
                "serverbound": {
                  "minecraft:intention": { "protocol_id": 0 }
                }
              }
            }
            """);
        var data = new VanillaGeneratedData(root.toString());
        var catalog = VanillaPacketCatalog.load(data);
        assertEquals(3, catalog.size());
        assertEquals(26, catalog.requireId(
                VanillaProtocol26_2.State.PLAY,
                VanillaProtocol26_2.Direction.CLIENTBOUND,
                "minecraft:disconnect"));
        assertEquals(12, catalog.requireId(
                VanillaProtocol26_2.State.PLAY,
                VanillaProtocol26_2.Direction.SERVERBOUND,
                "minecraft:chat"));
    }
}
