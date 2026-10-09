package dev.grimholt.server;

import dev.grimholt.server.lifecycle.LifecycleState;
import dev.grimholt.server.vanilla.VanillaProtocol26_2;
import dev.grimholt.server.vanilla.VanillaProtocolCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test for the startup ordering race between network ingress and
 * gameplay bootstrap. Every client begins its status handshake at the same
 * barrier immediately after the public bootstrap returns.
 */
class VanillaServerKernelStartupRaceTest {
    private static final int CONCURRENT_CLIENTS = 50;

    @TempDir
    Path tempDir;

    @Test
    void kernelAndWorldAreReadyBeforeConcurrentClientsCanReachSocket() throws Exception {
        Grimholt server = new Grimholt();
        Path config = tempDir.resolve("grimholt.properties");
        Files.writeString(config,
                "config-version=2\n" +
                "bind-address=127.0.0.1\n" +
                "port=0\n" +
                "online-mode=false\n" +
                "max-players=1000\n" +
                "dispatcher-threads=2\n" +
                "view-distance=10\n" +
                "simulation-distance=10\n" +
                "world-directory=" + tempDir.resolve("world").toString().replace("\\", "/") + "\n");

        try {
            server.start(config);

            assertEquals(LifecycleState.RUNNING, server.state());
            assertTrue(server.vanillaKernel().running(), "kernel must be running before ingress is reachable");
            assertTrue(server.vanillaKernel().worldCount() > 0, "world must be registered before ingress is reachable");
            assertTrue(server.network().boundPort() > 0, "network socket must be bound after bootstrap");

            CountDownLatch ready = new CountDownLatch(CONCURRENT_CLIENTS);
            CountDownLatch start = new CountDownLatch(1);
            List<java.util.concurrent.Future<?>> clients = new ArrayList<>(CONCURRENT_CLIENTS);

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int i = 0; i < CONCURRENT_CLIENTS; i++) {
                    clients.add(executor.submit(() -> {
                        ready.countDown();
                        if (!start.await(10, TimeUnit.SECONDS)) {
                            throw new AssertionError("client start barrier timed out");
                        }
                        assertStatusHandshake(server);
                        return null;
                    }));
                }

                assertTrue(ready.await(10, TimeUnit.SECONDS), "all clients must be ready to connect");
                start.countDown();
                for (var client : clients) client.get(20, TimeUnit.SECONDS);
            }
        } finally {
            server.stop();
        }
    }

    private static void assertStatusHandshake(Grimholt server) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", server.boundPort())) {
            socket.setSoTimeout(10_000);

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
            String json = VanillaProtocolCodec.readString(
                    new ByteArrayInputStream(response.payload()), 32767);
            assertTrue(json.contains("\\"name\\":\\"26.4-snapshot-3\\""),
                    "status must return the pinned protocol version: " + json);
            assertTrue(json.contains("\\"protocol\\":" + VanillaProtocol26_2.PROTOCOL_VERSION),
                    "status must return the pinned protocol number: " + json);
        }
    }
}
