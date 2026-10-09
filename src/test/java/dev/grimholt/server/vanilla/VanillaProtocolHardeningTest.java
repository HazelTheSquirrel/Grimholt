package dev.grimholt.server.vanilla;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class VanillaProtocolHardeningTest {
    @Test
    void rejectsTruncatedFrameBodyAndLengthVarInt() {
        assertThrows(IOException.class, () -> VanillaProtocol.decodeFrame(
                new ByteArrayInputStream(new byte[]{3, 0, 1}), 1024));
        assertThrows(IOException.class, () -> VanillaProtocol.decodeFrame(
                new ByteArrayInputStream(new byte[]{(byte) 0x80}), 1024));
    }

    @Test
    void rejectsZeroAndOversizedFrameLengths() {
        assertThrows(IOException.class, () -> VanillaProtocol.decodeFrame(
                new ByteArrayInputStream(new byte[]{0}), 1024));
        assertThrows(IOException.class, () -> VanillaProtocol.decodeFrame(
                new ByteArrayInputStream(new byte[]{(byte) 0xff, 0x07}), 64));
    }

    @Test
    void compressedPayloadMustMatchDeclaredInflatedLength() throws Exception {
        byte[] encoded = VanillaProtocol.encodeFrame(
                new VanillaProtocol.Frame(3, new byte[]{1, 2, 3, 4}), 1, 1024);
        // Outer length and the small, one-byte uncompressed-length VarInt precede zlib data.
        encoded[1] = (byte) (encoded[1] + 1);
        assertThrows(IOException.class, () -> VanillaProtocol.decodeFrame(
                new ByteArrayInputStream(encoded), 1024, 1));
    }

    @Test
    void compressedPayloadRejectsTruncatedZlibStream() throws Exception {
        byte[] encoded = VanillaProtocol.encodeFrame(
                new VanillaProtocol.Frame(3, new byte[]{1, 2, 3, 4, 5}), 1, 1024);
        byte[] truncated = Arrays.copyOf(encoded, encoded.length - 1);
        truncated[0]--;
        assertThrows(IOException.class, () -> VanillaProtocol.decodeFrame(
                new ByteArrayInputStream(truncated), 1024, 1));
    }

    @Test
    void compressionThresholdIsAppliedToPacketIdPlusPayload() throws Exception {
        // Packet ID contributes to the protocol's uncompressed packet length.
        var below = new VanillaProtocol.Frame(1, new byte[]{7, 8});
        byte[] encoded = VanillaProtocol.encodeFrame(below, 4, 1024);
        assertEquals(below, VanillaProtocol.decodeFrame(new ByteArrayInputStream(encoded), 1024, 4));
        var atThreshold = new VanillaProtocol.Frame(1, new byte[]{7, 8, 9});
        byte[] compressed = VanillaProtocol.encodeFrame(atThreshold, 4, 1024);
        assertEquals(atThreshold, VanillaProtocol.decodeFrame(new ByteArrayInputStream(compressed), 1024, 4));
    }

    @Test
    void varIntRoundTripsSignedProtocolValuesAndRejectsTruncation() throws Exception {
        for (int value : new int[]{0, 1, 127, 128, 255, 2_097_151, Integer.MAX_VALUE, -1, Integer.MIN_VALUE}) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            VanillaProtocol.writeVarInt(out, value);
            assertEquals(value, VanillaProtocol.readVarInt(new ByteArrayInputStream(out.toByteArray())));
        }
        assertThrows(IOException.class, () -> VanillaProtocol.readVarInt(
                new ByteArrayInputStream(new byte[]{(byte) 0x80, (byte) 0x80})));
    }
}
