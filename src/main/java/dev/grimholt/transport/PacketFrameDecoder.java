package dev.grimholt.transport;

import dev.grimholt.protocol.VarInt;
import java.io.IOException;
import java.util.Objects;

/** Decodes length-prefixed frames from a caller-owned buffer without copying payloads. */
public final class PacketFrameDecoder {
    private PacketFrameDecoder() { }

    @FunctionalInterface
    public interface FrameConsumer {
        /** The payload view is borrowed and must not be retained after this callback. */
        void accept(byte[] buffer, int offset, int length) throws IOException;
    }

    /** Returns the bytes consumed, leaving any incomplete final frame untouched. */
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
            int prefixLength = tryVarIntLength(buffer, prefixStart, end);
            if (prefixLength == 0) return prefixStart - offset;
            // Parse only after the prefix is complete; signed -1 is a valid VarInt value,
            // so it must not double as the incomplete-input sentinel here.
            int packetLength = VarInt.read(buffer, prefixStart, end);
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

    /** Returns zero for a truncated prefix; rejects a fifth continuation byte. */
    private static int tryVarIntLength(byte[] buffer, int offset, int limit) {
        for (int index = 0; index < 5; index++) {
            int cursor = offset + index;
            if (cursor >= limit) return 0;
            int current = buffer[cursor] & 0xff;
            if (index == 4 && (current & 0x80) != 0) {
                throw new IllegalArgumentException("Frame length VarInt exceeds five bytes");
            }
            if ((current & 0x80) == 0) return index + 1;
        }
        throw new IllegalArgumentException("Malformed frame length VarInt");
    }
}
