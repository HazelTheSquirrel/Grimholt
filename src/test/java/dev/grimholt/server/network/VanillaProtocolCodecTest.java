package dev.grimholt.server.network;

import dev.grimholt.server.vanilla.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class VanillaProtocolCodecTest {
    @Test void booleansAndVarLongRoundTrip() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        VanillaProtocolCodec.writeBoolean(out, true);
        VanillaProtocolCodec.writeBoolean(out, false);
        VanillaProtocolCodec.writeVarLong(out, 9_876_543_210L);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        assertTrue(VanillaProtocolCodec.readBoolean(in));
        assertFalse(VanillaProtocolCodec.readBoolean(in));
        assertEquals(9_876_543_210L, VanillaProtocolCodec.readVarLong(in));
    }

    @Test void compressionRoundTripPreservesFrame() throws Exception {
        VanillaProtocol26_2.Frame frame = new VanillaProtocol26_2.Frame(
                0x2a, "a".repeat(1024).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        byte[] encoded = VanillaProtocol26_2.encodeFrame(frame, 32, 2 * 1024 * 1024);
        VanillaProtocol26_2.Frame decoded = VanillaProtocol26_2.decodeFrame(
                new ByteArrayInputStream(encoded), 2 * 1024 * 1024, 32);
        assertEquals(frame, decoded);
        assertTrue(encoded.length < frame.payload().length);
    }

    @Test void compressionLeavesSmallPacketUncompressed() throws Exception {
        VanillaProtocol26_2.Frame frame = new VanillaProtocol26_2.Frame(1, new byte[] {1, 2, 3});
        byte[] encoded = VanillaProtocol26_2.encodeFrame(frame, 32, 1024);
        ByteArrayInputStream in = new ByteArrayInputStream(encoded);
        int frameLength = VanillaProtocol26_2.readVarInt(in);
        byte[] body = in.readNBytes(frameLength);
        ByteArrayInputStream compressedBody = new ByteArrayInputStream(body);
        assertEquals(0, VanillaProtocol26_2.readVarInt(compressedBody));
        assertArrayEquals(new byte[] {1, 2, 3}, compressedBody.readAllBytes());
        assertEquals(frame, VanillaProtocol26_2.decodeFrame(new ByteArrayInputStream(encoded), 1024, 32));
    }

    @Test void networkNbtHasUnnamedRoot() {
        byte[] encoded = VanillaProtocolCodec.writeNetworkNbtCompoundBytes(
                VanillaNbt.compound(Map.of("value", VanillaNbt.integer(42))));

        // Network NBT (1.20.2+) omits the root name. The compound payload
        // immediately starts with the first named child tag.
        byte[] expected = {
                VanillaNbt.COMPOUND,
                VanillaNbt.INT, 0, 5, 'v', 'a', 'l', 'u', 'e',
                0, 0, 0, 42,
                VanillaNbt.END
        };
        assertArrayEquals(expected, encoded);
    }
}
