package dev.grimholt.server;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bounded executor for Grimholt-owned non-tick work.
 *
 * Rejected work completes exceptionally. It is never executed inline on the submitter,
 * which protects Minestom tick/network threads from accidental expensive fallback work.
 */
public final class BoundedTaskExecutor implements AutoCloseable {
    private final ThreadPoolExecutor executor;

    public BoundedTaskExecutor(RuntimeProfile profile) {
        Objects.requireNonNull(profile, "profile");
        this.executor = new ThreadPoolExecutor(
                profile.backgroundParallelism(),
                profile.backgroundParallelism(),
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(profile.maxQueuedTasks()),
                namedDaemonFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    public <T> CompletableFuture<T> submit(Callable<T> task) {
        Objects.requireNonNull(task, "task");
        final CompletableFuture<T> result = new CompletableFuture<>();
        try {
            executor.execute(() -> {
                try {
                    result.complete(task.call());
                } catch (Throwable failure) {
                    result.completeExceptionally(failure);
                }
            });
        } catch (RejectedExecutionException rejected) {
            result.completeExceptionally(rejected);
        }
        return result;
    }

    public int queuedTaskCount() {
        return executor.getQueue().size();
    }

    public int activeTaskCount() {
        return executor.getActiveCount();
    }

    @Override
    public void close() {
        // Do not wait on the caller. Shutdown policy can be tightened when persistence is wired in.
        executor.shutdownNow();
    }

    private static ThreadFactory namedDaemonFactory() {
        final AtomicInteger sequence = new AtomicInteger();
        return task -> {
            final Thread thread = Thread.ofPlatform()
                    .daemon(true)
                    .name("grimholt-worker-" + sequence.incrementAndGet())
                    .unstarted(task);
            return thread;
        };
    }
}
