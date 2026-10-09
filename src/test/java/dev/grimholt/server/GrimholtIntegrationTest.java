package dev.grimholt.server;

import org.junit.jupiter.api.Test;
import dev.grimholt.server.vanilla.VanillaProtocol26_2;
import dev.grimholt.server.vanilla.VanillaProtocolCodec;
import dev.grimholt.server.vanilla.VanillaGeneratedData;
import dev.grimholt.server.vanilla.VanillaPacketCatalog;
import dev.grimholt.server.vanilla.VanillaSnapshot;
import java.util.UUID;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

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
        try {
            server.start(config);
            assertEquals(dev.grimholt.server.lifecycle.LifecycleState.RUNNING, server.state());
            assertNativeStatusHandshake(server);
            assertOfflineLoginReachesWorld(server);
        } finally { server.stop(); }
        assertEquals(dev.grimholt.server.lifecycle.LifecycleState.STOPPED,server.state());
    }

    private static void assertOfflineLoginReachesWorld(Grimholt server) throws Exception {
        VanillaPacketCatalog catalog = VanillaPacketCatalog.load(new VanillaGeneratedData());
        try (Socket socket = new Socket("127.0.0.1", server.boundPort())) {
            socket.setSoTimeout(15_000);

            ByteArrayOutputStream handshake = new ByteArrayOutputStream();
            VanillaProtocol26_2.writeVarInt(handshake, VanillaProtocol26_2.PROTOCOL_VERSION);
            VanillaProtocolCodec.writeString(handshake, "127.0.0.1", 255);
            new DataOutputStream(handshake).writeShort(server.boundPort());
            VanillaProtocol26_2.writeVarInt(handshake, 2);
            writeFrame(socket, new VanillaProtocol26_2.Frame(0, handshake.toByteArray()), -1);

            ByteArrayOutputStream loginStart = new ByteArrayOutputStream();
            VanillaProtocolCodec.writeString(loginStart, "GrimholtTest", 16);
            VanillaProtocolCodec.writeUuid(loginStart, UUID.nameUUIDFromBytes(
                    "GrimholtTest".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            writeFrame(socket, new VanillaProtocol26_2.Frame(
                    catalog.requireId(VanillaProtocol26_2.State.LOGIN,
                            VanillaProtocol26_2.Direction.SERVERBOUND, "minecraft:hello"),
                    loginStart.toByteArray()), -1);

            VanillaProtocol26_2.Frame compression = readFrame(socket, -1);
            assertEquals(catalog.requireId(VanillaProtocol26_2.State.LOGIN,
                    VanillaProtocol26_2.Direction.CLIENTBOUND, "minecraft:login_compression"),
                    compression.packetId(), "server must negotiate compression before login success");
            int threshold = VanillaProtocol26_2.readVarInt(new ByteArrayInputStream(compression.payload()));
            assertEquals(256, threshold);

            VanillaProtocol26_2.Frame success = readFrame(socket, threshold);
            assertEquals(catalog.requireId(VanillaProtocol26_2.State.LOGIN,
                    VanillaProtocol26_2.Direction.CLIENTBOUND, "minecraft:login_finished"),
                    success.packetId(), "offline login must complete");

            writeFrame(socket, new VanillaProtocol26_2.Frame(
                    catalog.requireId(VanillaProtocol26_2.State.LOGIN,
                            VanillaProtocol26_2.Direction.SERVERBOUND, "minecraft:login_acknowledged"),
                    new byte[0]), threshold);

            VanillaProtocol26_2.Frame knownPacksRequest = readFrame(socket, threshold);
            assertEquals(catalog.requireId(VanillaProtocol26_2.State.CONFIGURATION,
                    VanillaProtocol26_2.Direction.CLIENTBOUND, "minecraft:select_known_packs"),
                    knownPacksRequest.packetId());

            ByteArrayOutputStream knownPacks = new ByteArrayOutputStream();
            VanillaProtocol26_2.writeVarInt(knownPacks, 1);
            VanillaProtocolCodec.writeString(knownPacks, "minecraft", 32767);
            VanillaProtocolCodec.writeString(knownPacks, "core", 32767);
            VanillaProtocolCodec.writeString(knownPacks, VanillaSnapshot.VERSION, 32767);
            writeFrame(socket, new VanillaProtocol26_2.Frame(
                    catalog.requireId(VanillaProtocol26_2.State.CONFIGURATION,
                            VanillaProtocol26_2.Direction.SERVERBOUND, "minecraft:select_known_packs"),
                    knownPacks.toByteArray()), threshold);

            int registryPackets = 0;
            boolean featureFlags = false;
            boolean tags = false;
            int finishConfigurationId = catalog.requireId(VanillaProtocol26_2.State.CONFIGURATION,
                    VanillaProtocol26_2.Direction.CLIENTBOUND, "minecraft:finish_configuration");
            while (true) {
                VanillaProtocol26_2.Frame frame = readFrame(socket, threshold);
                if (frame.packetId() == catalog.requireId(VanillaProtocol26_2.State.CONFIGURATION,
                        VanillaProtocol26_2.Direction.CLIENTBOUND, "minecraft:registry_data")) registryPackets++;
                if (frame.packetId() == catalog.requireId(VanillaProtocol26_2.State.CONFIGURATION,
                        VanillaProtocol26_2.Direction.CLIENTBOUND, "minecraft:update_enabled_features")) featureFlags = true;
                if (frame.packetId() == catalog.requireId(VanillaProtocol26_2.State.CONFIGURATION,
                        VanillaProtocol26_2.Direction.CLIENTBOUND, "minecraft:update_tags")) tags = true;
                if (frame.packetId() == finishConfigurationId) break;
            }
            assertTrue(registryPackets >= 20, "configuration must contain the Snapshot 3 registries");
            assertTrue(featureFlags, "configuration must advertise enabled features");
            assertTrue(tags, "configuration must synchronize vanilla tags");

            writeFrame(socket, new VanillaProtocol26_2.Frame(
                    catalog.requireId(VanillaProtocol26_2.State.CONFIGURATION,
                            VanillaProtocol26_2.Direction.SERVERBOUND, "minecraft:finish_configuration"),
                    new byte[0]), threshold);

            int playLoginId = catalog.requireId(VanillaProtocol26_2.State.PLAY,
                    VanillaProtocol26_2.Direction.CLIENTBOUND, "minecraft:login");
            int chunkId = catalog.requireId(VanillaProtocol26_2.State.PLAY,
                    VanillaProtocol26_2.Direction.CLIENTBOUND, "minecraft:level_chunk_with_light");
            boolean playLogin = false;
            boolean firstChunk = false;
            while (!firstChunk) {
                VanillaProtocol26_2.Frame frame = readFrame(socket, threshold);
                if (frame.packetId() == playLoginId) playLogin = true;
                if (frame.packetId() == chunkId) firstChunk = true;
            }
            assertTrue(playLogin, "client must receive the PLAY login packet");
            assertTrue(firstChunk, "client must receive at least one initial world chunk");
        }
    }

    private static void writeFrame(Socket socket, VanillaProtocol26_2.Frame frame, int compressionThreshold)
            throws Exception {
        socket.getOutputStream().write(VanillaProtocol26_2.encodeFrame(frame, compressionThreshold, 2 * 1024 * 1024));
        socket.getOutputStream().flush();
    }

    private static VanillaProtocol26_2.Frame readFrame(Socket socket, int compressionThreshold) throws Exception {
        return VanillaProtocol26_2.decodeFrame(socket.getInputStream(), 2 * 1024 * 1024, compressionThreshold);
    }

    private static void assertNativeStatusHandshake(Grimholt server) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", server.boundPort())) {
            socket.setSoTimeout(5000);
            ByteArrayOutputStream handshake = new ByteArrayOutputStream();
            VanillaProtocol26_2.writeVarInt(handshake, VanillaProtocol26_2.PROTOCOL_VERSION);
            VanillaProtocolCodec.writeString(handshake, "127.0.0.1", 255);
            new DataOutputStream(handshake).writeShort(server.boundPort());
            VanillaProtocol26_2.writeVarInt(handshake, 1);

            socket.getOutputStream().write(VanillaProtocol26_2.encodeFrame(
                    new VanillaProtocol26_2.Frame(0, handshake.toByteArray())));
            socket.getOutputStream().write(VanillaProtocol26_2.encodeFrame(
                    new VanillaProtocol26_2.Frame(0, new byte[0])));
            socket.getOutputStream().flush();

            VanillaProtocol26_2.Frame response = VanillaProtocol26_2.decodeFrame(
                    socket.getInputStream(), 32 * 1024);
            assertEquals(0, response.packetId(), "status response packet ID");
            ByteArrayInputStream payload = new ByteArrayInputStream(response.payload());
            String json = VanillaProtocolCodec.readString(payload, 32767);
            assertTrue(json.contains("\"name\":\"26.4-snapshot-3\""),
                    "native Grimholt status response must advertise the pinned version: " + json);
            assertTrue(json.contains("\"protocol\":" + VanillaProtocol26_2.PROTOCOL_VERSION),
                    "native Grimholt status response must advertise the pinned protocol: " + json);
        }
    }
}
