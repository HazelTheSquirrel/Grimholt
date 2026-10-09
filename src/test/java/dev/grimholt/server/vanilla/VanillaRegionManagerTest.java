package dev.grimholt.server.vanilla;

import dev.grimholt.server.concurrency.RegionManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class VanillaRegionManagerTest {
    @Test
    void regionStateIsOwnedAndTicksThroughTheRegionBoundary() {
        ArrayDeque<Runnable> queue = new ArrayDeque<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        VanillaRegionManager manager = new VanillaRegionManager(16, queue::add, failure::set);
        UUID world = UUID.randomUUID();

        VanillaRegionRuntime runtime = manager.region(world, 0, 0);
        runtime.owner().execute(() -> {
            VanillaChunk chunk = runtime.chunk(0, 0);
            chunk.setBlock(new BlockPos(1, 64, 1), BlockState.of("minecraft:stone"));
            runtime.tick();
            assertEquals(1, chunk.ticker().tickCount());
        });
        assertEquals(1, queue.size());
        queue.remove().run();

        assertEquals(1, manager.regionCount());
        assertNull(failure.get());
        assertTrue(queue.isEmpty());
        manager.close();
    }

    @Test
    void newlyCreatedRegionChunksHaveAValidBootstrapSurface() {
        ArrayDeque<Runnable> queue = new ArrayDeque<>();
        VanillaRegionManager manager = new VanillaRegionManager(16, queue::add, failure -> fail(failure));
        UUID world = UUID.randomUUID();
        VanillaRegionRuntime runtime = manager.region(world, 0, 0);
        AtomicReference<VanillaChunk> chunkRef = new AtomicReference<>();

        runtime.owner().execute(() -> chunkRef.set(runtime.chunk(0, 0)));
        queue.remove().run();

        VanillaChunk chunk = chunkRef.get();
        VanillaOverworldGenerator generator = new VanillaOverworldGenerator(0L);
        int surface = generator.surfaceY(0, 0);
        runtime.owner().execute(() -> {
            assertTrue(chunk.loaded());
            assertEquals("minecraft:grass_block", chunk.block(new BlockPos(0, surface, 0)).id());
            assertEquals("minecraft:dirt", chunk.block(new BlockPos(0, surface - 1, 0)).id());
            assertEquals("minecraft:bedrock",
                    chunk.block(new BlockPos(0, VanillaChunk.MIN_SECTION_Y * 16, 0)).id());
        });

        manager.close();
    }

    @Test
    void crossOwnerWorkIsQueuedAndExecutedByTheRegionScheduler() {
        ArrayDeque<Runnable> queue = new ArrayDeque<>();
        VanillaRegionManager manager = new VanillaRegionManager(16, queue::add, throwable -> fail(throwable));
        UUID world = UUID.randomUUID();
        VanillaRegionRuntime runtime = manager.region(world, 0, 0);

        AtomicReference<VanillaChunk> chunk = new AtomicReference<>();
        runtime.owner().execute(() -> chunk.set(runtime.chunk(0, 0)));

        manager.execute(world, 0, 0, () ->
                chunk.get().setBlock(new BlockPos(0, 64, 0), BlockState.of("minecraft:dirt")));

        assertEquals(1, queue.size());
        queue.remove().run();

        runtime.owner().execute(() ->
                assertEquals("minecraft:dirt", chunk.get().block(new BlockPos(0, 64, 0)).id()));

        manager.close();
    }
}
