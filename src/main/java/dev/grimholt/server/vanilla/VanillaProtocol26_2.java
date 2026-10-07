package dev.grimholt.server.vanilla;

import java.io.*;
import java.util.*;

/**
 * Minecraft Java 26.2 transport primitives.
 *
 * <p>Minestom's packet classes are deliberately not treated as the protocol
 * contract. This codec owns the wire framing rules used by Grimholt and pins
 * the negotiated protocol version to 1073742165. Packet IDs/codecs are loaded
 * from the exact generated reference catalog when available.</p>
 */
public final class VanillaProtocol26_2 {
    public static final int PROTOCOL_VERSION = VanillaSnapshot26_2.PROTOCOL;
    public static final int MAX_VARINT_BYTES = 5;

    public enum State { HANDSHAKE, STATUS, LOGIN, CONFIGURATION, PLAY }

    public record Frame(int packetId, byte[] payload) {
        @Override public boolean equals(Object other) {
            return other instanceof Frame frame && packetId == frame.packetId && Arrays.equals(payload, frame.payload);
        }
        @Override public int hashCode() { return 31 * Integer.hashCode(packetId) + Arrays.hashCode(payload); }
        @Override public String toString() { return "Frame[" + packetId + ", payload=" + Arrays.toString(payload) + "]"; }
        public Frame {
            if (packetId < 0) throw new IllegalArgumentException("packetId");
            Objects.requireNonNull(payload, "payload");
        }
    }

    public record PacketKey(State state, Direction direction, int id) {}
    public enum Direction { CLIENTBOUND, SERVERBOUND }

    public static int readVarInt(InputStream in) throws IOException {
        int result = 0;
        int shift = 0;
        for (int i = 0; i < MAX_VARINT_BYTES; i++) {
            int b = in.read();
            if (b < 0) throw new EOFException("Unexpected EOF in VarInt");
            result |= (b & 0x7f) << shift;
            if ((b & 0x80) == 0) return result;
            shift += 7;
        }
        throw new IOException("VarInt exceeds " + MAX_VARINT_BYTES + " bytes");
    }

    public static void writeVarInt(OutputStream out, int value) throws IOException {
        int v = value;
        do {
            int next = v & 0x7f;
            v >>>= 7;
            if (v != 0) next |= 0x80;
            out.write(next);
        } while (v != 0);
    }

    public static byte[] encodeFrame(Frame frame) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writeVarInt(body, frame.packetId());
        body.write(frame.payload());
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        writeVarInt(result, body.size());
        body.writeTo(result);
        return result.toByteArray();
    }

    public static Frame decodeFrame(InputStream in, int maxFrameBytes) throws IOException {
        int length = readVarInt(in);
        if (length < 1 || length > maxFrameBytes) {
            throw new IOException("Invalid packet frame length: " + length);
        }
        byte[] body = in.readNBytes(length);
        if (body.length != length) throw new EOFException("Truncated packet frame");
        ByteArrayInputStream packet = new ByteArrayInputStream(body);
        int id = readVarInt(packet);
        return new Frame(id, packet.readAllBytes());
    }

    public static void requireProtocol(int protocol) {
        if (protocol != PROTOCOL_VERSION) {
            throw new IllegalStateException(
                "Client protocol " + protocol + " is not Minecraft 26.2 (" +
                PROTOCOL_VERSION + ")");
        }
    }

    public static String packetCatalogPath() {
        return "reports/packets.json";
    }
}
