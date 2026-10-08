package dev.grimholt.server;

import org.junit.jupiter.api.Test;
import dev.grimholt.server.vanilla.VanillaProtocol26_2;
import dev.grimholt.server.vanilla.VanillaProtocolCodec;
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
        } finally { server.stop(); }
        assertEquals(dev.grimholt.server.lifecycle.LifecycleState.STOPPED,server.state());
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
            assertTrue(json.contains("\\\"name\\\":\\\"26.4-snapshot-3\\\""),
                    "native Grimholt status response must advertise the pinned version: " + json);
            assertTrue(json.contains("\\\"protocol\\\":" + VanillaProtocol26_2.PROTOCOL_VERSION),
                    "native Grimholt status response must advertise the pinned protocol: " + json);
        }
    }
}
