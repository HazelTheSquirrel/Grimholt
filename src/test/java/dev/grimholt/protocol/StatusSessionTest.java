package dev.grimholt.protocol;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusSessionTest {
    private static final StatusResponse RESPONSE = new StatusResponse("hello", "Grimholt Dev", 767, 512, 0);

    @Test
    void handshakeTransitionsIntoStatusWithoutOutput() throws IOException {
        StatusSession session = new StatusSession(RESPONSE);
        byte[] handshake = handshake(1);
        assertEquals(0, session.handleFrame(handshake, 0, handshake.length, new byte[1024], 0));
        assertEquals(StatusSession.State.STATUS, session.state());
    }

    @Test
    void statusRequestProducesFramedJsonResponse() throws IOException {
        StatusSession session = new StatusSession(RESPONSE);
        byte[] output = new byte[1024];
        handshakeStatus(session, output);
        int written = session.handleFrame(new byte[]{0}, 0, 1, output, 0);
        int frameLength = VarInt.read(output, 0, written);
        int prefixLength = VarInt.sizeOf(frameLength);
        assertEquals(written - prefixLength, frameLength);
        assertEquals(0, VarInt.read(output, prefixLength, written));
        String json = new String(output, prefixLength + 2, written - prefixLength - 2, StandardCharsets.UTF_8);
        assertTrue(json.contains("Grimholt Dev"));
        assertEquals(StatusSession.State.STATUS, session.state());
    }

    @Test
    void pingEchoesTokenAndClosesAfterWrite() throws IOException {
        StatusSession session = new StatusSession(RESPONSE);
        byte[] output = new byte[64];
        handshakeStatus(session, output);
        byte[] ping = new byte[9];
        ping[0] = 1;
        long token = 0x8123456789ABCDEFL;
        for (int i = 0; i < 8; i++) ping[i + 1] = (byte) (token >>> (56 - 8 * i));
        int written = session.handleFrame(ping, 0, ping.length, output, 0);
        assertEquals(10, written);
        assertEquals(token, StatusPacketCodec.readPingPayload(output, 1, written - 1));
        assertEquals(StatusSession.State.CLOSE_AFTER_WRITE, session.state());
        assertThrows(IOException.class, () -> session.handleFrame(new byte[]{0}, 0, 1, output, written));
    }

    @Test
    void rejectsLoginAndMalformedStatusRequest() throws IOException {
        StatusSession login = new StatusSession(RESPONSE);
        byte[] loginHandshake = handshake(2);
        assertThrows(IOException.class,
                () -> login.handleFrame(loginHandshake, 0, loginHandshake.length, new byte[32], 0));
        assertEquals(StatusSession.State.HANDSHAKE, login.state());

        StatusSession status = new StatusSession(RESPONSE);
        byte[] output = new byte[256];
        handshakeStatus(status, output);
        assertThrows(IOException.class, () -> status.handleFrame(new byte[]{0, 1}, 0, 2, output, 0));
    }

    private static void handshakeStatus(StatusSession session, byte[] output) throws IOException {
        byte[] handshake = handshake(1);
        session.handleFrame(handshake, 0, handshake.length, output, 0);
    }

    private static byte[] handshake(int nextState) {
        byte[] address = "localhost".getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[1 + 2 + 1 + address.length + 2 + 1];
        int cursor = 0;
        payload[cursor++] = 0;
        payload[cursor++] = (byte) 0xff;
        payload[cursor++] = 0x05; // protocol version 767
        payload[cursor++] = (byte) address.length;
        System.arraycopy(address, 0, payload, cursor, address.length);
        cursor += address.length;
        payload[cursor++] = 0x63;
        payload[cursor++] = (byte) 0xdd; // port 25565
        payload[cursor] = (byte) nextState;
        return payload;
    }
}
