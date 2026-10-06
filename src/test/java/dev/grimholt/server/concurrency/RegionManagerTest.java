package dev.grimholt.server.concurrency;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RegionManagerTest {
    @Test
    void mapsChunksDeterministically() {
        UUID world = UUID.randomUUID();
        RegionManager manager = new RegionManager(8, Runnable::run, t -> {});
        assertSame(manager.region(world, 0, 0), manager.region(world, 7, 7));
        assertSame(manager.region(world, -1, -1), manager.region(world, -8, -8));
        assertNotSame(manager.region(world, 0, 0), manager.region(world, 8, 0));
        manager.close();
    }

    @Test
    void executesThroughRegionScheduler() {
        AtomicInteger value = new AtomicInteger();
        RegionManager manager = new RegionManager(8, Runnable::run, t -> {});
        manager.execute(UUID.randomUUID(), 0, 0, () -> value.incrementAndGet());
        assertEquals(1, value.get());
        manager.close();
    }
}
