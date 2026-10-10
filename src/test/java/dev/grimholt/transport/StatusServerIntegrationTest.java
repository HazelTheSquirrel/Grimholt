package dev.grimholt.transport;

import dev.grimholt.protocol.StatusResponse;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusServerIntegrationTest {
    @Test
    void handlesFragmentedHandshakeStatusAndPingOverTcp() throws Exception {
        StatusServer server = new StatusServer("127.0.0.1", 0,
                new StatusResponse("integration test", "Grimholt Dev", 767, 512, 0));
        AtomicReference<Throwable> serverFailure = new AtomicReference<>();
        Thread serverThread = Thread.ofPlatform().name("status-server-test").start(() -> {
            try {
                server.run();
            } catch (Throwable failure) {
                serverFailure.set(failure);
            }
        });

        try (Socket socket = new Socket()) {
            socket.connect(server.localAddress());
            socket.setSoTimeout(2_000);
            DataInputStream input = new DataInputStream(socket.getInputStream());

            byte[] handshakeFrame = frame(handshake());
            byte[] requestFrame = new byte[]{1, 0};
            byte[] exchange = new byte[handshakeFrame.length + requestFrame.length];
            System.arraycopy(handshakeFrame, 0, exchange, 0, handshakeFrame.length);
            System.arraycopy(requestFrame, 0, exchange, handshakeFrame.length, requestFrame.length);

            socket.getOutputStream().write(exchange, 0, 4);
            socket.getOutputStream().flush();
            socket.getOutputStream().write(exchange, 4, exchange.length - 4);
            socket.getOutputStream().flush();

            byte[] status = readFrame(input);
            assertEquals(0, status[0] & 0xff);
            int[] lengthField = readVarInt(status, 1);
            int jsonLength = lengthField[0];
            String json = new String(status, lengthField[1], jsonLength, StandardCharsets.UTF_8);
            assertTrue(json.contains("integration test"));

            long token = 0x8123456789ABCDEFL;
            byte[] pingPayload = new byte[9];
            pingPayload[0] = 1;
            for (int i = 0; i < 8; i++) pingPayload[i + 1] = (byte) (token >>> (56 - 8 * i));
            socket.getOutputStream().write(frame(pingPayload));
            socket.getOutputStream().flush();

            byte[] pong = readFrame(input);
            assertEquals(1, pong[0] & 0xff);
            long echoed = 0;
            for (int i = 1; i < 9; i++) echoed = (echoed << 8) | (pong[i] & 0xffL);
            assertEquals(token, echoed);
            assertEquals(-1, input.read(), "server should close after flushing pong");
        } finally {
            server.close();
            serverThread.join(2_000);
        }

        assertFalse(serverThread.isAlive(), "selector thread should stop cleanly");
        if (serverFailure.get() != null) throw new AssertionError("server loop failed", serverFailure.get());
    }

    private static byte[] readFrame(DataInputStream input) throws IOException {
        int length = input.readUnsignedByte();
        byte[] frame = input.readNBytes(length);
        assertEquals(length, frame.length, "complete frame should be received");
        return frame;
    }

    private static int[] readVarInt(byte[] bytes, int offset) {
        int value = 0;
        int cursor = offset;
        for (int shift = 0; shift < 35; shift += 7) {
            int current = bytes[cursor++] & 0xff;
            value |= (current & 0x7f) << shift;
            if ((current & 0x80) == 0) return new int[]{value, cursor};
        }
        throw new AssertionError("invalid VarInt in server response");
    }

    private static byte[] frame(byte[] payload) {
        if (payload.length >= 128) throw new IllegalArgumentException("test payload expects one-byte length");
        byte[] framed = new byte[payload.length + 1];
        framed[0] = (byte) payload.length;
        System.arraycopy(payload, 0, framed, 1, payload.length);
        return framed;
    }

    private static byte[] handshake() {
        byte[] address = "localhost".getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[1 + 2 + 1 + address.length + 2 + 1];
        int cursor = 0;
        payload[cursor++] = 0;
        payload[cursor++] = (byte) 0xff;
        payload[cursor++] = 0x05;
        payload[cursor++] = (byte) address.length;
        System.arraycopy(address, 0, payload, cursor, address.length);
        cursor += address.length;
        payload[cursor++] = 0x63;
        payload[cursor++] = (byte) 0xdd;
        payload[cursor] = 1;
        return payload;
    }
}
