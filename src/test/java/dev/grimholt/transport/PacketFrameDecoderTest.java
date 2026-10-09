package dev.grimholt.transport;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PacketFrameDecoderTest {
    @Test void decodesMultipleFramesWithoutCopyingPayloads() throws IOException {
        byte[] input = {2, 0x10, 0x11, 1, 0x22};
        List<Integer> payloads = new ArrayList<>();
        List<Integer> lengths = new ArrayList<>();
        int consumed = PacketFrameDecoder.decodeAvailable(input, 0, input.length, 16,
                (buffer, offset, length) -> {
                    lengths.add(length);
                    payloads.add(buffer[offset] & 0xff);
                });
        assertEquals(input.length, consumed);
        assertEquals(List.of(2, 1), lengths);
        assertEquals(List.of(0x10, 0x22), payloads);
    }

    @Test void leavesAnIncompleteFrameUnconsumed() throws IOException {
        byte[] input = {1, 0x10, 3, 0x20};
        List<Integer> payloads = new ArrayList<>();
        int consumed = PacketFrameDecoder.decodeAvailable(input, 0, input.length, 16,
                (buffer, offset, length) -> payloads.add(buffer[offset] & 0xff));
        assertEquals(2, consumed);
        assertEquals(List.of(0x10), payloads);
    }

    @Test void acceptsIncompleteHeaderWithoutConsumingIt() throws IOException {
        byte[] input = {(byte) 0x80};
        assertEquals(0, PacketFrameDecoder.decodeAvailable(input, 0, input.length, 16,
                (buffer, offset, length) -> { throw new AssertionError("No complete frame expected"); }));
    }

    @Test void rejectsOversizedEmptyAndNegativeFramesBeforeDispatch() {
        assertThrows(IOException.class, () -> PacketFrameDecoder.decodeAvailable(new byte[]{17, 1}, 0, 2, 16,
                (buffer, offset, length) -> { throw new AssertionError("Oversized frame dispatched"); }));
        assertThrows(IOException.class, () -> PacketFrameDecoder.decodeAvailable(new byte[]{0}, 0, 1, 16,
                (buffer, offset, length) -> { throw new AssertionError("Empty frame dispatched"); }));
        byte[] negativeLength = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x0f};
        assertThrows(IOException.class, () -> PacketFrameDecoder.decodeAvailable(negativeLength, 0, 5, 16,
                (buffer, offset, length) -> { throw new AssertionError("Negative frame dispatched"); }));
    }
}
