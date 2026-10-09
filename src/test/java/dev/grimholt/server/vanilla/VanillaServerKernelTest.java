package dev.grimholt.server.vanilla;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import dev.grimholt.server.runtime.ChronosRegionScheduler;
import java.util.concurrent.TimeUnit;

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
    void ownsPlayerLifecycleAndPosition() {
        try (VanillaServerKernel kernel = new VanillaServerKernel(8, Runnable::run, ignored -> {})) {
            kernel.start();
            UUID world = UUID.randomUUID();
            UUID player = UUID.randomUUID();
            kernel.registerWorld(world);

            kernel.updatePlayerPosition(world, player, 12.5, 70.0, -4.25, 90.0f, 15.0f, true);
            assertEquals(1, kernel.playerCount());
            VanillaPlayerState state = kernel.player(player);
            assertNotNull(state);
            assertEquals(12.5, state.x());
            assertEquals(70.0, state.y());
            assertEquals(-4.25, state.z());
            assertEquals(90.0f, state.yaw());
            assertTrue(state.onGround());

            assertSame(state, kernel.removePlayer(player));
            assertEquals(0, kernel.playerCount());
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
    @Test
    void productionKernelTicksRegionsThroughChronos() throws Exception {
        try (ChronosRegionScheduler chronos = new ChronosRegionScheduler(2, 16, 10);
             VanillaServerKernel kernel = new VanillaServerKernel(16, chronos, ignored -> fail("unexpected region failure"))) {
            kernel.start();
            UUID world = UUID.randomUUID();
            kernel.registerWorld(world);
            VanillaRegionRuntime region = kernel.region(world, 0, 0);

            kernel.startRegionTicks();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (region.game().tickCount() == 0 && System.nanoTime() < deadline) {
                Thread.sleep(5);
            }

            assertTrue(region.game().tickCount() > 0,
                    "production kernel should tick registered regions via Chronos");
            assertThrows(IllegalStateException.class, kernel::tick,
                    "Chronos mode must not allow the old global tickAll path");
        }
    }

}
