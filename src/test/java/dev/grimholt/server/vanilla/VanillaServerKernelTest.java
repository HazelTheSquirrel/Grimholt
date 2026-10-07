package dev.grimholt.server.vanilla;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class VanillaServerKernelTest {
    @Test
    void ownsWorldStateOutsideMinestom() {
        Executor executor = Runnable::run;
        AtomicInteger failures = new AtomicInteger();
        try (VanillaServerKernel kernel = new VanillaServerKernel(32, executor::execute,
                ignored -> failures.incrementAndGet())) {
            assertFalse(kernel.running());
            kernel.start();

            UUID world = UUID.randomUUID();
            kernel.registerWorld(world);

            assertTrue(kernel.running());
            assertEquals(1, kernel.worldCount());

            VanillaRegionRuntime region = kernel.region(world, 0, 0);
            assertNotNull(region);
            assertEquals(1, kernel.regionCount());

            kernel.execute(world, 0, 0, () -> region.chunk(0, 0).setBlock(
                    new BlockPos(0, 64, 0), BlockState.of("minecraft:stone")));

            AtomicReference<String> blockId = new AtomicReference<>();
            kernel.execute(world, 0, 0, () -> blockId.set(
                    region.chunk(0, 0).block(new BlockPos(0, 64, 0)).id()));
            assertEquals("minecraft:stone", blockId.get());
            assertEquals(0, failures.get());
        }
    }

    @Test
    void rejectsUnknownWorlds() {
        try (VanillaServerKernel kernel = new VanillaServerKernel(8, Runnable::run,
                ignored -> {})) {
            kernel.start();
            UUID world = UUID.randomUUID();
            assertThrows(IllegalArgumentException.class, () -> kernel.region(world, 0, 0));
        }
    }

    @Test
    void shutdownIsIdempotent() {
        try (VanillaServerKernel kernel = new VanillaServerKernel(8, Runnable::run,
                ignored -> {})) {
            kernel.start();
            kernel.stop();
            kernel.stop();
            assertFalse(kernel.running());
            assertEquals(0, kernel.worldCount());
        }
    }
}
