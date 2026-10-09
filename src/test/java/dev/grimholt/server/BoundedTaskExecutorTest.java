package dev.grimholt.server;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoundedTaskExecutorTest {
    @Test
    void runsSubmittedWorkOnWorkerThread() throws Exception {
        final RuntimeProfile profile = RuntimeProfile.from(2, 2L << 30);
        try (var executor = new BoundedTaskExecutor(profile)) {
            final String threadName = executor.submit(() -> Thread.currentThread().getName())
                    .get(2, TimeUnit.SECONDS);
            assertEquals("grimholt-worker-1", threadName);
        }
    }

    @Test
    void reportsTaskFailuresThroughFuture() {
        final RuntimeProfile profile = RuntimeProfile.from(2, 2L << 30);
        try (var executor = new BoundedTaskExecutor(profile)) {
            final var future = executor.submit(() -> {
                throw new IllegalStateException("expected");
            });
            assertThrows(ExecutionException.class, () -> future.get(2, TimeUnit.SECONDS));
        }
    }
}
