package dev.grimholt.runtime;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Fixed-size executor with bounded admission; saturated work is rejected, never caller-run. */
public final class BoundedTaskExecutor implements AutoCloseable {
    private final ThreadPoolExecutor executor;

    public BoundedTaskExecutor(String name, int workers, int queueCapacity) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        if (workers < 1 || queueCapacity < 1) throw new IllegalArgumentException("capacities must be positive");
        AtomicInteger sequence = new AtomicInteger();
        ThreadFactory factory = task -> {
            Thread thread = new Thread(task, name + "-" + sequence.incrementAndGet());
            thread.setDaemon(false);
            thread.setUncaughtExceptionHandler((failed, error) ->
                    System.err.println("Uncaught error on " + failed.getName() + ": " + error));
            return thread;
        };
        executor = new ThreadPoolExecutor(workers, workers, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity), factory, new ThreadPoolExecutor.AbortPolicy());
    }

    /** @return true if accepted, false if saturated or shutdown has started */
    public boolean tryExecute(Runnable task) {
        Objects.requireNonNull(task, "task");
        try {
            executor.execute(task);
            return true;
        } catch (RejectedExecutionException rejected) {
            return false;
        }
    }

    public int queuedTasks() { return executor.getQueue().size(); }
    public int activeTasks() { return executor.getActiveCount(); }
    public boolean isShutdown() { return executor.isShutdown(); }

    public void shutdown(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isNegative()) throw new IllegalArgumentException("timeout must not be negative");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeout.toNanos(), TimeUnit.NANOSECONDS)) executor.shutdownNow();
        } catch (InterruptedException interrupted) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override public void close() { shutdown(Duration.ofSeconds(5)); }
}
