package dev.grimholt.runtime;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedTaskExecutorTest {
    @Test void rejectsWorkWhenWorkerAndQueueAreSaturated() throws InterruptedException {
        BoundedTaskExecutor executor = new BoundedTaskExecutor("test-worker", 1, 1);
        CountDownLatch running = new CountDownLatch(1); CountDownLatch release = new CountDownLatch(1);
        try {
            assertTrue(executor.tryExecute(() -> {
                running.countDown();
                try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }));
            assertTrue(running.await(2, TimeUnit.SECONDS));
            assertTrue(executor.tryExecute(() -> { }));
            assertFalse(executor.tryExecute(() -> { }));
        } finally { release.countDown(); executor.shutdown(Duration.ofSeconds(2)); }
        assertFalse(executor.tryExecute(() -> { }));
    }
    @Test void neverRunsRejectedWorkOnSubmittingThread() throws InterruptedException {
        BoundedTaskExecutor executor = new BoundedTaskExecutor("test-worker", 1, 1);
        CountDownLatch running = new CountDownLatch(1); CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean ranOnCaller = new AtomicBoolean();
        try {
            executor.tryExecute(() -> {
                running.countDown();
                try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            assertTrue(running.await(2, TimeUnit.SECONDS));
            executor.tryExecute(() -> { });
            assertFalse(executor.tryExecute(() -> ranOnCaller.set(true)));
            assertFalse(ranOnCaller.get());
        } finally { release.countDown(); executor.shutdown(Duration.ofSeconds(2)); }
    }
}
