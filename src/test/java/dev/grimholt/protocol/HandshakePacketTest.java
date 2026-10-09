package dev.grimholt.protocol;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HandshakePacketTest {
    @Test
    void parsesStatusHandshakeWithUnsignedPort() throws IOException {
        byte[] payload = handshake(765, "play.example.test", 25565, 1);
        HandshakePacket parsed = HandshakePacket.parse(payload, 0, payload.length);

        assertEquals(765, parsed.protocolVersion());
        assertEquals("play.example.test", parsed.serverAddress());
        assertEquals(25565, parsed.serverPort());
        assertEquals(HandshakePacket.NextState.STATUS, parsed.nextState());
    }

    @Test
    void parsesLoginHandshakeFromNonZeroSlice() throws IOException {
        byte[] packet = handshake(765, "localhost", 0xffff, 2);
        byte[] framed = new byte[packet.length + 4];
        System.arraycopy(packet, 0, framed, 2, packet.length);

        HandshakePacket parsed = HandshakePacket.parse(framed, 2, packet.length);
        assertEquals(65535, parsed.serverPort());
        assertEquals(HandshakePacket.NextState.LOGIN, parsed.nextState());
    }

    @Test
    void rejectsUnsupportedStateAndTrailingBytes() {
        byte[] unsupported = handshake(765, "localhost", 25565, 3);
        assertThrows(IOException.class, () -> HandshakePacket.parse(unsupported, 0, unsupported.length));

        byte[] valid = handshake(765, "localhost", 25565, 1);
        byte[] trailing = java.util.Arrays.copyOf(valid, valid.length + 1);
        assertThrows(IOException.class, () -> HandshakePacket.parse(trailing, 0, trailing.length));
    }

    @Test
    void rejectsTruncatedAddressAndInvalidUtf8() {
        byte[] truncated = {0, (byte) 0x80};
        assertThrows(IOException.class, () -> HandshakePacket.parse(truncated, 0, truncated.length));

        byte[] invalidUtf8 = {0, 1, 1, (byte) 0xff, 0x63, (byte) 0xdd, 1};
        assertThrows(IOException.class, () -> HandshakePacket.parse(invalidUtf8, 0, invalidUtf8.length));
    }

    private static byte[] handshake(int protocolVersion, String address, int port, int nextState) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeVarInt(out, 0); // Handshake packet id
        writeVarInt(out, protocolVersion);
        byte[] encodedAddress = address.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, encodedAddress.length);
        out.writeBytes(encodedAddress);
        out.write((port >>> 8) & 0xff);
        out.write(port & 0xff);
        writeVarInt(out, nextState);
        return out.toByteArray();
    }

    private static void writeVarInt(ByteArrayOutputStream out, int value) {
        byte[] encoded = new byte[5];
        int length = VarInt.write(value, encoded, 0);
        out.write(encoded, 0, length);
    }
}
