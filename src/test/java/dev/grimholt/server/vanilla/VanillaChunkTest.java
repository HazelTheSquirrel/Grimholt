package dev.grimholt.server.vanilla;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VanillaChunkTest {
    @Test
    void chunkTracksSparseBlockAndFluidState() {
        VanillaChunk chunk = new VanillaChunk(-3, 7);
        assertFalse(chunk.loaded());
        chunk.load();

        BlockPos pos = new BlockPos(2, 64, 3);
        chunk.setBlock(pos, BlockState.of("minecraft:stone"));
        chunk.setFluid(pos, new VanillaFluidState("minecraft:water", 0, false));

        assertTrue(chunk.loaded());
        assertEquals("minecraft:stone", chunk.block(pos).id());
        assertTrue(chunk.fluid(pos).source());
        assertEquals(1, chunk.blockSnapshot().size());
        assertEquals(1, chunk.fluidSnapshot().size());

        chunk.setFluid(pos, VanillaFluidState.empty());
        assertTrue(chunk.fluid(pos).isEmpty());
    }
}
