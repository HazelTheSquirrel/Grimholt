package dev.grimholt.transport;

import dev.grimholt.protocol.VarInt;

import java.io.IOException;
import java.util.Objects;

/**
 * Decodes length-prefixed packet frames from a caller-owned byte buffer.
 * Payload bytes are borrowed for the duration of the callback; no frame or payload copy is made.
 */
public final class PacketFrameDecoder {
    private PacketFrameDecoder() { }

    @FunctionalInterface
    public interface FrameConsumer {
        /** The payload view is valid only while the decoder's input buffer remains unchanged. */
        void accept(byte[] buffer, int offset, int length) throws IOException;
    }

    /**
     * Decodes as many complete frames as possible and returns bytes consumed.
     * If the final frame is incomplete, its first byte is not consumed so the caller can retain it.
     * Packet lengths are checked before invoking the consumer.
     */
    public static int decodeAvailable(byte[] buffer, int offset, int available, int maxPacketLength,
                                      FrameConsumer consumer) throws IOException {
        Objects.requireNonNull(buffer, "buffer");
        Objects.requireNonNull(consumer, "consumer");
        if (offset < 0 || available < 0 || offset > buffer.length - available) {
            throw new IndexOutOfBoundsException("Invalid input range");
        }
        if (maxPacketLength < 1) throw new IllegalArgumentException("maxPacketLength must be positive");

        int end = offset + available;
        int cursor = offset;
        while (cursor < end) {
            int prefixStart = cursor;
            int packetLength = VarInt.read(buffer, prefixStart, end);
            if (packetLength == -1) return prefixStart - offset;

            int prefixLength = varIntLength(buffer, prefixStart, end);
            if (packetLength <= 0) throw new IOException("Packet frame length must be positive");
            if (packetLength > maxPacketLength) {
                throw new IOException("Packet frame exceeds configured maximum: " + packetLength);
            }
            int payloadStart = prefixStart + prefixLength;
            // Subtraction avoids overflow from payloadStart + packetLength on hostile input.
            if (packetLength > end - payloadStart) return prefixStart - offset;

            consumer.accept(buffer, payloadStart, packetLength);
            cursor = payloadStart + packetLength;
        }
        return cursor - offset;
    }

    private static int varIntLength(byte[] buffer, int offset, int limit) {
        for (int cursor = offset; cursor < limit && cursor - offset < 5; cursor++) {
            if ((buffer[cursor] & 0x80) == 0) return cursor - offset + 1;
        }
        throw new IllegalArgumentException("Incomplete or malformed frame length");
    }
}
