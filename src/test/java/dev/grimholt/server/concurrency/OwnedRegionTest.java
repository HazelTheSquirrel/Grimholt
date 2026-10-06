package dev.grimholt.server.concurrency;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class OwnedRegionTest {
    @Test
    void crossOwnerWorkIsExplicitlyHandedOff() throws Exception {
        var scheduled = new AtomicInteger();
        var tasks = new java.util.concurrent.ConcurrentLinkedQueue<Runnable>();
        var region = new OwnedRegion(new RegionKey(java.util.UUID.randomUUID(), 0, 0), 8, task -> {
            scheduled.incrementAndGet();
            tasks.add(task);
        });

        var done = new CountDownLatch(1);
        var pool = Executors.newSingleThreadExecutor();
        try {
            pool.submit(() -> region.execute(() -> done.countDown())).get();
            assertEquals(1, scheduled.get());
            assertEquals(1, tasks.size());
            assertFalse(done.await(20, TimeUnit.MILLISECONDS));
            tasks.remove().run();
            assertTrue(done.await(1, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
            region.close();
        }
    }

    @Test
    void handoffQueueIsBounded() {
        var region = new OwnedRegion(new RegionKey(java.util.UUID.randomUUID(), 0, 0), 1, task -> {});
        try {
            region.execute(() -> {});
            assertThrows(java.util.concurrent.RejectedExecutionException.class, () -> region.execute(() -> {}));
        } finally {
            region.close();
        }
    }

    @Test
    void closedRegionRejectsNewWork() {
        var region = new OwnedRegion(new RegionKey(java.util.UUID.randomUUID(), 0, 0), 1, task -> {});
        region.close();
        assertThrows(java.util.concurrent.RejectedExecutionException.class, () -> region.execute(() -> {}));
    }

    @Test
    void tickOwnsStateExclusively() {
        var region = new OwnedRegion(new RegionKey(java.util.UUID.randomUUID(), 0, 0), 2, Runnable::run);
        var value = new AtomicInteger();
        region.execute(value::incrementAndGet);
        assertEquals(1, value.get());
    }
}
