package dev.grimholt.protocol;

import java.io.IOException;
import java.util.Objects;

/**
 * Codec for the Minecraft STATUS response and ping/pong packets.
 *
 * <p>Methods write framed packets into caller-owned storage. No packet buffer is
 * allocated by the codec; callers retain control over buffer pooling and socket writes.</p>
 */
public final class StatusPacketCodec {
    private static final int STATUS_RESPONSE_PACKET_ID = 0;
    private static final int PING_PACKET_ID = 1;
    private static final int PONG_PACKET_ID = 1;
    private static final int PING_PAYLOAD_LENGTH = 8;

    private StatusPacketCodec() { }

    /**
     * Encodes a STATUS response packet into {@code destination}.
     *
     * @return total frame bytes written, including the outer VarInt length
     */
    public static int encodeStatusResponse(String json, byte[] destination, int offset) {
        Objects.requireNonNull(json, "json");
        Objects.requireNonNull(destination, "destination");
        checkOffset(destination.length, offset);

        // Status is not a per-tick hot path; encode once per request, then write directly
        // into the connection's reusable output buffer instead of allocating a packet array.
        byte[] jsonBytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int payloadLength = 1 + VarInt.sizeOf(jsonBytes.length) + jsonBytes.length;
        int cursor = offset;
        cursor += VarInt.write(payloadLength, destination, cursor);
        cursor += VarInt.write(STATUS_RESPONSE_PACKET_ID, destination, cursor);
        cursor += VarInt.write(jsonBytes.length, destination, cursor);
        if (jsonBytes.length > destination.length - cursor) {
            throw new IndexOutOfBoundsException("Destination is too small for status response");
        }
        System.arraycopy(jsonBytes, 0, destination, cursor, jsonBytes.length);
        cursor += jsonBytes.length;
        return cursor - offset;
    }

    /**
     * Parses the 8-byte payload from a client ping packet (packet ID 0x01).
     * The payload is returned unchanged so the caller can echo it in the pong.
     */
    public static long readPingPayload(byte[] payload, int offset, int length) throws IOException {
        Objects.requireNonNull(payload, "payload");
        if (offset < 0 || length < 0 || offset > payload.length - length) {
            throw new IndexOutOfBoundsException("Invalid ping payload range");
        }
        int limit = offset + length;
        final int packetId;
        try {
            packetId = VarInt.read(payload, offset, limit);
        } catch (IllegalArgumentException malformed) {
            throw new IOException("Malformed ping packet id", malformed);
        }
        if (packetId != PING_PACKET_ID) throw new IOException("Expected ping packet id 1");
        int packetIdLength = VarInt.sizeOf(packetId);
        if (limit - offset - packetIdLength != PING_PAYLOAD_LENGTH) {
            throw new IOException("Ping packet must contain exactly eight payload bytes");
        }

        int cursor = offset + packetIdLength;
        // Network byte order: the most significant byte is transmitted first.
        long value = 0L;
        for (int i = 0; i < PING_PAYLOAD_LENGTH; i++) {
            value = (value << 8) | (payload[cursor + i] & 0xffL);
        }
        return value;
    }

    /**
     * Encodes a framed pong packet with the exact payload value received in the ping.
     *
     * @return total frame bytes written, including the outer VarInt length
     */
    public static int encodePong(long payload, byte[] destination, int offset) {
        Objects.requireNonNull(destination, "destination");
        checkOffset(destination.length, offset);
        int cursor = offset;
        cursor += VarInt.write(1 + PING_PAYLOAD_LENGTH, destination, cursor);
        cursor += VarInt.write(PONG_PACKET_ID, destination, cursor);
        // Eight shifts write the long in big-endian/network byte order.
        for (int shift = 56; shift >= 0; shift -= 8) {
            if (cursor >= destination.length) {
                throw new IndexOutOfBoundsException("Destination is too small for pong packet");
            }
            destination[cursor++] = (byte) (payload >>> shift);
        }
        return cursor - offset;
    }

    private static void checkOffset(int capacity, int offset) {
        if (offset < 0 || offset > capacity) {
            throw new IndexOutOfBoundsException("Invalid destination offset");
        }
    }
}
