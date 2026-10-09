package dev.grimholt.server.runtime;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ChronosRegionSchedulerTest {
    @Test
    void differentRegionsRunInParallel() throws Exception {
        try (ChronosRegionScheduler scheduler = new ChronosRegionScheduler(2, 8, 50)) {
            CountDownLatch started = new CountDownLatch(2);
            CountDownLatch release = new CountDownLatch(1);
            try (var first = scheduler.register("0,0", () -> await(release, started));
                 var second = scheduler.register("1,0", () -> await(release, started))) {
                scheduler.dispatchEpoch();
                assertTrue(started.await(2, TimeUnit.SECONDS),
                        "independent regions should start on separate workers");
                release.countDown();
            }
        }
    }

    @Test
    void slowRegionDoesNotAccumulateCatchUpTicks() throws Exception {
        try (ChronosRegionScheduler scheduler = new ChronosRegionScheduler(1, 4, 50)) {
            CountDownLatch entered = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            try (var region = scheduler.register("hot-region", () -> {
                entered.countDown();
                await(release, new CountDownLatch(0));
            })) {
                scheduler.dispatchEpoch();
                assertTrue(entered.await(2, TimeUnit.SECONDS));
                scheduler.dispatchEpoch();
                scheduler.dispatchEpoch();

                var metrics = scheduler.metrics("hot-region");
                assertNotNull(metrics);
                assertEquals(2, metrics.skippedBusyTicks(),
                        "a region with an in-flight tick must be coalesced, not backlogged");

                release.countDown();
            }
        }
    }

    @Test
    void failuresAreCountedAndDoNotBreakLaterTicks() throws Exception {
        try (ChronosRegionScheduler scheduler = new ChronosRegionScheduler(1, 4, 50)) {
            AtomicInteger calls = new AtomicInteger();
            CountDownLatch failureReported = new CountDownLatch(1);
            try (var region = scheduler.register("faulty-region", () -> {
                if (calls.incrementAndGet() == 1) throw new IllegalStateException("expected test failure");
            })) {
                region.onFailure(ignored -> failureReported.countDown());
                scheduler.dispatchEpoch();
                assertTrue(failureReported.await(2, TimeUnit.SECONDS));
                scheduler.dispatchEpoch();

                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
                while (scheduler.metrics("faulty-region").completedTicks() < 1
                        && System.nanoTime() < deadline) {
                    Thread.onSpinWait();
                }
                assertEquals(1, scheduler.metrics("faulty-region").failedTicks());
                assertEquals(1, scheduler.metrics("faulty-region").completedTicks());
            }
        }
    }

    @Test
    void rejectsInvalidConfigurationAndDuplicateRegionIds() {
        assertThrows(IllegalArgumentException.class, () -> new ChronosRegionScheduler(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new ChronosRegionScheduler(1, 0));
        try (ChronosRegionScheduler scheduler = new ChronosRegionScheduler(1, 1)) {
            scheduler.register("same", () -> {});
            assertThrows(IllegalArgumentException.class, () -> scheduler.register("same", () -> {}));
        }
    }

    private static void await(CountDownLatch release, CountDownLatch started) {
        started.countDown();
        try {
            if (!release.await(2, TimeUnit.SECONDS)) throw new AssertionError("test release timed out");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }
}
