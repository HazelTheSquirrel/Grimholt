package dev.grimholt.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VarIntTest {
    @Test void roundTripsBoundaryValues() {
        int[] values = {0, 1, 127, 128, 255, 16_384, Integer.MAX_VALUE, -1, Integer.MIN_VALUE};
        for (int value : values) {
            byte[] bytes = new byte[5];
            int written = VarInt.write(value, bytes, 0);
            assertEquals(VarInt.sizeOf(value), written);
            assertEquals(value, VarInt.read(bytes, 0, written));
        }
    }

    @Test void distinguishesIncompleteInputFromMalformedInput() {
        assertEquals(-1, VarInt.read(new byte[]{(byte) 0x80}, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> VarInt.read(new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x10}, 0, 5));
    }

    @Test void respectsDestinationBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> VarInt.write(128, new byte[1], 0));
    }
}
