package dev.grimholt.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockPositionTest {
    @Test void mapsNegativeCoordinatesUsingFloorArithmetic() {
        BlockPosition position = new BlockPosition(-1, 64, -17);
        assertEquals(-1, position.chunkKey().x()); assertEquals(-2, position.chunkKey().z());
        assertEquals(15, position.localX()); assertEquals(15, position.localZ());
    }
    @Test void mapsChunkBoundaries() {
        assertEquals(0, new BlockPosition(15, 0, 15).chunkKey().x());
        assertEquals(1, new BlockPosition(16, 0, 16).chunkKey().x());
        assertEquals(0, new BlockPosition(-16, 0, -16).localX());
        assertEquals(-1, new BlockPosition(-16, 0, -16).chunkKey().x());
    }
}
