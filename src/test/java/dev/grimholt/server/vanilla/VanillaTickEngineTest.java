package dev.grimholt.server.vanilla;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VanillaTickEngineTest {
    @Test
    void advancesTimeAndPreservesPhaseOrder() {
        VanillaWorldState world = new VanillaWorldState();
        VanillaTickEngine engine = new VanillaTickEngine(world, 16);
        List<VanillaTickEngine.Phase> phases = new ArrayList<>();
        engine.onPhase(phases::add);

        engine.tick();
        engine.tick();

        assertEquals(2, engine.tickCount());
        assertEquals(2, world.time().gameTime());
        assertEquals(List.of(
                VanillaTickEngine.Phase.TIME,
                VanillaTickEngine.Phase.SCHEDULED_BLOCKS,
                VanillaTickEngine.Phase.RANDOM_TICKS,
                VanillaTickEngine.Phase.ENTITIES,
                VanillaTickEngine.Phase.PLAYERS,
                VanillaTickEngine.Phase.NETWORK,
                VanillaTickEngine.Phase.TIME,
                VanillaTickEngine.Phase.SCHEDULED_BLOCKS,
                VanillaTickEngine.Phase.RANDOM_TICKS,
                VanillaTickEngine.Phase.ENTITIES,
                VanillaTickEngine.Phase.PLAYERS,
                VanillaTickEngine.Phase.NETWORK), phases);
    }

    @Test
    void blockStatesRoundTrip() {
        VanillaWorldState world = new VanillaWorldState();
        BlockPos pos = new BlockPos(1, 64, -3);
        BlockState state = new BlockState("minecraft:stone", java.util.Map.of("axis", "x"));
        world.setBlock(pos, state);
        assertEquals(state, world.getBlock(pos));
        assertEquals("minecraft:air", world.getBlock(new BlockPos(2, 64, 2)).id());
    }
    @Test
    void scheduledBlockTicksRunWhenDue() {
        VanillaWorldState world = new VanillaWorldState();
        VanillaTickEngine engine = new VanillaTickEngine(world, 16);
        List<BlockPos> processed = new ArrayList<>();
        engine.onScheduledBlockTick(processed::add);
        BlockPos pos = new BlockPos(3, 70, 4);
        engine.scheduledBlocks().schedule(2, pos);

        engine.tick();
        assertTrue(processed.isEmpty());
        engine.tick();
        assertEquals(List.of(pos), processed);
        assertEquals(0, engine.scheduledBlocks().pending());
    }

}
