package dev.grimholt.server.network;

import dev.grimholt.server.vanilla.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.Arrays;
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
        assertEquals(0, encoded[1] == VanillaNbt.END ? 1 : 0);
        assertTrue(Arrays.equals(expected, encoded));
    }
}
