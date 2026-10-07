
package dev.grimholt.server.api;

import dev.grimholt.api.*;
import dev.grimholt.server.runtime.GrimholtResourceProfile;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BoundedScheduler implements Scheduler, AutoCloseable {
    private final ScheduledThreadPoolExecutor timer;
    private final ThreadPoolExecutor async;
    private final Semaphore admission;
    private volatile java.util.function.Consumer<Runnable> tickExecutor;

    public BoundedScheduler(int workers, int maxQueued) {
        if (workers < 1 || maxQueued < 1) throw new IllegalArgumentException();
        admission = new Semaphore(maxQueued);
        timer = new ScheduledThreadPoolExecutor(
                Math.max(1, Math.min(workers, 4)),
                namedFactory("Grimholt-Tick"));
        timer.setRemoveOnCancelPolicy(true);
        tickExecutor = timer::execute;
        async = new ThreadPoolExecutor(
                workers, workers, 30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(maxQueued),
                namedFactory("Grimholt-Worker"),
                new ThreadPoolExecutor.AbortPolicy());
    }

    public static BoundedScheduler automatic(int maxQueued) {
        GrimholtResourceProfile profile = GrimholtResourceProfile.detect();
        int workers = Math.max(1, profile.workerParallelism());
        return new BoundedScheduler(workers, maxQueued);
    }

    public int workerCount() { return async.getCorePoolSize(); }

    private static ThreadFactory namedFactory(String prefix) {
        return new ThreadFactory() {
            private final java.util.concurrent.atomic.AtomicInteger sequence = new java.util.concurrent.atomic.AtomicInteger();
            @Override public Thread newThread(Runnable task) {
                Thread thread = new Thread(task, prefix + "-" + sequence.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        };
    }

    private void acquire() {
        if (!admission.tryAcquire()) throw new RejectedExecutionException("Grimholt scheduler queue is full");
    }

    public Task run(Runnable task) { return tickOneShot(task); }
    public Task runAsync(Runnable task) { return oneShot(task, true, 0); }

    void bindTickExecutor(java.util.function.Consumer<Runnable> executor) {
        this.tickExecutor = java.util.Objects.requireNonNull(executor, "executor");
    }

    private Task tickOneShot(Runnable task) {
        acquire();
        var h = new H(false);
        Runnable work = () -> {
            try { if (!h.cancelled()) task.run(); }
            finally { h.release(); }
        };
        try { tickExecutor.accept(work); return h; }
        catch (RuntimeException e) { h.release(); throw e; }
    }

    private Task oneShot(Runnable task, boolean asyncRun, long delayNanos) {
        acquire();
        var h = new H(false);
        Runnable work = () -> {
            try { if (!h.cancelled()) task.run(); }
            finally { h.release(); }
        };
        try {
            if (delayNanos > 0) h.future = timer.schedule(work, delayNanos, TimeUnit.NANOSECONDS);
            else if (asyncRun) async.execute(work);
            else timer.execute(work);
            return h;
        } catch (RuntimeException e) { h.release(); throw e; }
    }

    public Task runLater(Duration d, Runnable task) {
        if (d.isNegative()) throw new IllegalArgumentException("delay < 0");
        return oneShot(task, false, d.toNanos());
    }

    public Task runRepeating(Duration initial, Duration period, Runnable task) {
        if (initial.isNegative() || period.isZero() || period.isNegative()) throw new IllegalArgumentException();
        acquire();
        var h = new H(true);
        try {
            h.future = timer.scheduleAtFixedRate(() -> {
                if (!h.cancelled()) try { task.run(); } catch (Throwable ignored) {}
            }, initial.toNanos(), period.toNanos(), TimeUnit.NANOSECONDS);
            return h;
        } catch (RuntimeException e) { h.release(); throw e; }
    }

    public void close() {
        timer.shutdownNow();
        async.shutdownNow();
        admission.drainPermits();
    }

    private final class H implements Task {
        private final boolean repeating;
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicBoolean released = new AtomicBoolean();
        private Future<?> future;

        H(boolean repeating) { this.repeating = repeating; }

        void release() {
            if (released.compareAndSet(false, true)) admission.release();
        }

        public boolean cancel() {
            if (!cancelled.compareAndSet(false, true)) return false;
            if (future != null) future.cancel(false);
            release();
            return true;
        }

        public boolean cancelled() { return cancelled.get(); }
    }
}
