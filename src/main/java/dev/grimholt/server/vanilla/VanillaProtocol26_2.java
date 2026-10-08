package dev.grimholt.server.vanilla;

import java.io.*;
import java.util.*;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Grimholt-owned Minecraft 26.4-snapshot-3 transport primitives.
 *
 * <p>Packet IDs/codecs remain data-driven from Mojang's generated packet report.
 * This class owns framing, VarInts and the optional zlib packet-compression layer.</p>
 */
public final class VanillaProtocol26_2 {
    public static final int PROTOCOL_VERSION = VanillaSnapshot26_2.PROTOCOL;
    public static final int MAX_VARINT_BYTES = 5;

    public enum State { HANDSHAKE, STATUS, LOGIN, CONFIGURATION, PLAY }

    public record Frame(int packetId, byte[] payload) {
        @Override public boolean equals(Object other) {
            return other instanceof Frame frame && packetId == frame.packetId() && Arrays.equals(payload, frame.payload());
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
        return encodeFrame(frame, -1, Integer.MAX_VALUE);
    }

    public static byte[] encodeFrame(Frame frame, int compressionThreshold, int maxFrameBytes) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writeVarInt(body, frame.packetId());
        body.write(frame.payload());
        byte[] uncompressed = body.toByteArray();

        ByteArrayOutputStream framedBody = new ByteArrayOutputStream();
        if (compressionThreshold >= 0) {
            if (uncompressed.length >= compressionThreshold) {
                byte[] compressed = zlibCompress(uncompressed);
                writeVarInt(framedBody, uncompressed.length);
                framedBody.write(compressed);
            } else {
                writeVarInt(framedBody, 0);
                framedBody.write(uncompressed);
            }
        } else {
            framedBody.write(uncompressed);
        }

        if (framedBody.size() > maxFrameBytes) {
            throw new IOException("Encoded packet frame exceeds " + maxFrameBytes + " bytes");
        }
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        writeVarInt(result, framedBody.size());
        framedBody.writeTo(result);
        return result.toByteArray();
    }

    public static Frame decodeFrame(InputStream in, int maxFrameBytes) throws IOException {
        return decodeFrame(in, maxFrameBytes, -1);
    }

    public static Frame decodeFrame(InputStream in, int maxFrameBytes, int compressionThreshold) throws IOException {
        int length = readVarInt(in);
        if (length < 1 || length > maxFrameBytes) {
            throw new IOException("Invalid packet frame length: " + length);
        }
        byte[] body = in.readNBytes(length);
        if (body.length != length) throw new EOFException("Truncated packet frame");

        byte[] packetBytes;
        if (compressionThreshold >= 0) {
            ByteArrayInputStream compressedFrame = new ByteArrayInputStream(body);
            int uncompressedLength = readVarInt(compressedFrame);
            if (uncompressedLength == 0) {
                packetBytes = compressedFrame.readAllBytes();
                if (packetBytes.length >= compressionThreshold) {
                    throw new IOException("Uncompressed packet violates compression threshold");
                }
            } else {
                if (uncompressedLength < compressionThreshold || uncompressedLength > maxFrameBytes) {
                    throw new IOException("Invalid uncompressed packet length: " + uncompressedLength);
                }
                packetBytes = zlibDecompress(compressedFrame.readAllBytes(), uncompressedLength, maxFrameBytes);
            }
        } else {
            packetBytes = body;
        }

        ByteArrayInputStream packet = new ByteArrayInputStream(packetBytes);
        int id = readVarInt(packet);
        return new Frame(id, packet.readAllBytes());
    }

    private static byte[] zlibCompress(byte[] input) throws IOException {
        Deflater deflater = new Deflater();
        try {
            deflater.setInput(input);
            deflater.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(input.length);
            byte[] buffer = new byte[Math.min(8192, Math.max(256, input.length))];
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                if (count <= 0) throw new IOException("Zlib compressor made no progress");
                out.write(buffer, 0, count);
            }
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    private static byte[] zlibDecompress(byte[] input, int expectedLength, int maxFrameBytes) throws IOException {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(input);
            ByteArrayOutputStream out = new ByteArrayOutputStream(expectedLength);
            byte[] buffer = new byte[Math.min(8192, Math.max(256, expectedLength))];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count > 0) {
                    if (out.size() + count > expectedLength || out.size() + count > maxFrameBytes) {
                        throw new IOException("Inflated packet exceeds declared size");
                    }
                    out.write(buffer, 0, count);
                } else if (inflater.needsDictionary()) {
                    throw new IOException("Zlib packet requires a dictionary");
                } else if (inflater.needsInput()) {
                    throw new IOException("Truncated zlib packet");
                } else {
                    throw new IOException("Zlib decompressor made no progress");
                }
            }
            if (out.size() != expectedLength) {
                throw new IOException("Zlib packet length mismatch");
            }
            if (inflater.getRemaining() != 0) {
                throw new IOException("Trailing data after compressed packet");
            }
            return out.toByteArray();
        } catch (java.util.zip.DataFormatException e) {
            throw new IOException("Invalid zlib packet", e);
        } finally {
            inflater.end();
        }
    }

    /** Parsed handshake fields. The hostname is retained for diagnostics/routing only. */
    public record Handshake(String host, int port, int nextState) {
        public Handshake {
            Objects.requireNonNull(host, "host");
            if (port < 0 || port > 65535) throw new IllegalArgumentException("port");
            if (nextState != 1 && nextState != 2) throw new IllegalArgumentException("nextState");
        }
    }

    /** Strictly parses the initial handshake and rejects trailing or malformed payload data. */
    public static Handshake decodeHandshake(Frame frame) throws IOException {
        Objects.requireNonNull(frame, "frame");
        if (frame.packetId() != 0) throw new IOException("Expected handshake packet id 0");
        ByteArrayInputStream in = new ByteArrayInputStream(frame.payload());
        int protocol = readVarInt(in);
        try {
            requireProtocol(protocol);
        } catch (IllegalStateException mismatch) {
            throw new IOException(mismatch.getMessage(), mismatch);
        }
        String host = VanillaProtocolCodec.readString(in, 255);
        if (in.available() < 2) throw new EOFException("Handshake missing port");
        int port = (in.read() << 8) | in.read();
        int nextState = readVarInt(in);
        if (nextState != 1 && nextState != 2) {
            throw new IOException("Unsupported handshake next state: " + nextState);
        }
        if (in.available() != 0) throw new IOException("Trailing bytes after handshake packet");
        return new Handshake(host, port, nextState);
    }

    public static void requireProtocol(int protocol) {
        if (protocol != PROTOCOL_VERSION) {
            throw new IllegalStateException(
                    "Client protocol " + protocol + " is not Minecraft " + VanillaSnapshot26_2.VERSION +
                    " (" + PROTOCOL_VERSION + ")");
        }
    }

    public static String packetCatalogPath() {
        return "reports/packets.json";
    }
}
