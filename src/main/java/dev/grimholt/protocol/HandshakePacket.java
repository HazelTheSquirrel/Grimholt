package dev.grimholt.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Parsed client handshake packet (packet id 0x00).
 *
 * <p>The parser operates on a borrowed frame slice and creates only the address String
 * retained by the connection state. It rejects incomplete fields, invalid UTF-8,
 * unknown next states, and trailing bytes before a session can change state.</p>
 */
public record HandshakePacket(int protocolVersion, String serverAddress, int serverPort, NextState nextState) {
    private static final int MAX_ADDRESS_CHARS = 255;
    private static final int MAX_ADDRESS_BYTES = MAX_ADDRESS_CHARS * 4;

    public HandshakePacket {
        if (protocolVersion < 0) throw new IllegalArgumentException("protocolVersion must be non-negative");
        Objects.requireNonNull(serverAddress, "serverAddress");
        Objects.requireNonNull(nextState, "nextState");
        if (serverAddress.length() > MAX_ADDRESS_CHARS) {
            throw new IllegalArgumentException("serverAddress exceeds 255 characters");
        }
        if (serverPort < 0 || serverPort > 0xffff) {
            throw new IllegalArgumentException("serverPort must be an unsigned 16-bit value");
        }
    }

    public enum NextState {
        STATUS(1),
        LOGIN(2);

        private final int wireValue;

        NextState(int wireValue) {
            this.wireValue = wireValue;
        }

        public int wireValue() {
            return wireValue;
        }

        private static NextState fromWireValue(int value) throws IOException {
            return switch (value) {
                case 1 -> STATUS;
                case 2 -> LOGIN;
                default -> throw new IOException("Unsupported handshake next state: " + value);
            };
        }
    }

    /**
     * Parses exactly one handshake packet payload, excluding its outer frame length.
     * The byte slice is borrowed and is never retained.
     */
    public static HandshakePacket parse(byte[] payload, int offset, int length) throws IOException {
        Objects.requireNonNull(payload, "payload");
        if (offset < 0 || length < 0 || offset > payload.length - length) {
            throw new IndexOutOfBoundsException("Invalid handshake payload range");
        }
        int limit = offset + length;
        int cursor = offset;

        int packetId = readNonNegativeVarInt(payload, cursor, limit, "packet id");
        cursor += VarInt.sizeOf(packetId);
        if (packetId != 0) throw new IOException("Expected handshake packet id 0, got " + packetId);

        int protocolVersion = readNonNegativeVarInt(payload, cursor, limit, "protocol version");
        cursor += VarInt.sizeOf(protocolVersion);

        int addressLength = readNonNegativeVarInt(payload, cursor, limit, "server address length");
        cursor += VarInt.sizeOf(addressLength);
        if (addressLength > MAX_ADDRESS_BYTES) {
            throw new IOException("Server address exceeds maximum UTF-8 byte length");
        }
        if (addressLength > limit - cursor) throw new IOException("Truncated server address");

        final String address;
        try {
            // REPORT prevents malformed UTF-8 from silently being replaced with U+FFFD.
            address = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(payload, cursor, addressLength)).toString();
        } catch (CharacterCodingException malformed) {
            throw new IOException("Server address is not valid UTF-8", malformed);
        }
        if (address.length() > MAX_ADDRESS_CHARS) {
            throw new IOException("Server address exceeds 255 characters");
        }
        cursor += addressLength;

        if (limit - cursor < 2) throw new IOException("Truncated server port");
        int serverPort = ((payload[cursor] & 0xff) << 8) | (payload[cursor + 1] & 0xff);
        cursor += 2;

        int nextStateValue = readNonNegativeVarInt(payload, cursor, limit, "next state");
        cursor += VarInt.sizeOf(nextStateValue);
        if (cursor != limit) throw new IOException("Unexpected trailing bytes in handshake packet");

        return new HandshakePacket(protocolVersion, address, serverPort, NextState.fromWireValue(nextStateValue));
    }

    private static int readNonNegativeVarInt(byte[] payload, int offset, int limit, String field)
            throws IOException {
        final int value;
        try {
            value = VarInt.read(payload, offset, limit);
        } catch (IllegalArgumentException malformed) {
            throw new IOException("Malformed handshake " + field, malformed);
        }
        // All fields parsed here are non-negative; this also rejects truncated VarInts,
        // for which VarInt.read uses -1 as its incomplete-input sentinel.
        if (value < 0) throw new IOException("Missing or negative handshake " + field);
        return value;
    }
}
