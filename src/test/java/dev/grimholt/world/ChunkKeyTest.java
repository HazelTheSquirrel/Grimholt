package dev.grimholt.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkKeyTest {
    @Test void roundTripsSignedCoordinates() {
        assertRoundTrip(0, 0); assertRoundTrip(1, 1); assertRoundTrip(-1, -1);
        assertRoundTrip(-17, 29); assertRoundTrip(29, -17);
    }
    @Test void roundTripsIntegerBoundariesWithoutAliasing() {
        assertRoundTrip(Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertRoundTrip(Integer.MAX_VALUE, Integer.MIN_VALUE);
    }
    @Test void packedRepresentationIsStable() {
        assertEquals(0x0000_0002_0000_0003L, ChunkKey.of(2, 3).packed());
        assertEquals(-1L, ChunkKey.of(-1, -1).packed());
    }
    private static void assertRoundTrip(int x, int z) {
        ChunkKey key = ChunkKey.of(x, z);
        assertEquals(x, key.x()); assertEquals(z, key.z());
    }
}
