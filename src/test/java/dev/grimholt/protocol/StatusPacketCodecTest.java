package dev.grimholt.protocol;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusPacketCodecTest {
    @Test
    void statusResponseJsonEscapesTextAndContainsRequiredFields() {
        StatusResponse response = new StatusResponse("Grimholt \"test\"\\world\nready", "1.21.x", 767, 500, 12);
        String json = response.toJson();

        assertTrue(json.contains("\"protocol\":767"));
        assertTrue(json.contains("\"max\":500"));
        assertTrue(json.contains("\"online\":12"));
        assertTrue(json.contains("Grimholt \\\"test\\\"\\\\world\\nready"));
    }

    @Test
    void encodesFramedStatusResponseIntoAnOffsetBuffer() {
        byte[] output = new byte[512];
        int written = StatusPacketCodec.encodeStatusResponse("{\"description\":{\"text\":\"ok\"}}", output, 3);

        int frameLength = VarInt.read(output, 3, 3 + written);
        int framePrefixLength = VarInt.sizeOf(frameLength);
        assertEquals(written - framePrefixLength, frameLength);
        assertEquals(0, VarInt.read(output, 3 + framePrefixLength, 3 + written));
        assertTrue(new String(output, 3 + framePrefixLength + 2, written - framePrefixLength - 2,
                StandardCharsets.UTF_8).contains("ok"));
    }

    @Test
    void pingPayloadRoundTripsThroughPong() throws IOException {
        long token = 0xFEDCBA9876543210L;
        byte[] pingPayload = new byte[9];
        pingPayload[0] = 1;
        for (int i = 0; i < 8; i++) pingPayload[i + 1] = (byte) (token >>> (56 - i * 8));

        assertEquals(token, StatusPacketCodec.readPingPayload(pingPayload, 0, pingPayload.length));

        byte[] framedPong = new byte[12];
        int written = StatusPacketCodec.encodePong(token, framedPong, 2);
        assertEquals(9, VarInt.read(framedPong, 2, 2 + written));
        assertEquals(1, VarInt.read(framedPong, 3, 3 + written));
        assertEquals(token, StatusPacketCodec.readPingPayload(framedPong, 3, written - 1));
    }

    @Test
    void rejectsWrongPingIdAndIncorrectPayloadLength() {
        assertThrows(IOException.class,
                () -> StatusPacketCodec.readPingPayload(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0}, 0, 9));
        assertThrows(IOException.class,
                () -> StatusPacketCodec.readPingPayload(new byte[]{1, 2}, 0, 2));
    }

    @Test
    void rejectsImpossiblePlayerCounts() {
        assertThrows(IllegalArgumentException.class, () -> new StatusResponse("x", "v", 1, 5, 6));
    }
}
