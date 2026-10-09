package dev.grimholt.server.runtime;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Chronos: bounded, region-parallel tick dispatch owned by Grimholt.
 *
 * <p>The clock never performs gameplay work and never waits for workers.
 * Each logical region has at most one queued/running tick, so a slow region
 * cannot create an unbounded backlog of obsolete tick tasks. Different regions
 * may execute concurrently; a region's mutable state must still be guarded by
 * its single-owner contract. Cross-region mutations must be handed off through
 * the owning region rather than performed directly by a tick callback.</p>
 *
 * <p>This is a dispatch kernel, not a promise of 20 TPS under arbitrary load.
 * Overload is measured and skipped ticks are visible instead of being hidden
 * behind an ever-growing executor queue.</p>
 */
public final class ChronosRegionScheduler implements AutoCloseable {
    public static final long DEFAULT_TICK_MILLIS = 50L;

    private final ConcurrentHashMap<String, RegionTask> regions = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor workers;
    private final ScheduledExecutorService clock;
    private final long tickMillis;
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicLong epochs = new AtomicLong();
    private final LongAdder submittedTicks = new LongAdder();
    private final LongAdder completedTicks = new LongAdder();
    private final LongAdder skippedBusyTicks = new LongAdder();
    private final LongAdder rejectedTicks = new LongAdder();
    private final LongAdder failedTicks = new LongAdder();
    private final LongAdder totalTickNanos = new LongAdder();

    public ChronosRegionScheduler(int workerCount, int queueCapacity) {
        this(workerCount, queueCapacity, DEFAULT_TICK_MILLIS);
    }

    public ChronosRegionScheduler(int workerCount, int queueCapacity, long tickMillis) {
        if (workerCount < 1) throw new IllegalArgumentException("workerCount must be >= 1");
        if (queueCapacity < 1) throw new IllegalArgumentException("queueCapacity must be >= 1");
        if (tickMillis < 1) throw new IllegalArgumentException("tickMillis must be >= 1");
        this.tickMillis = tickMillis;

        ThreadFactory workerFactory = namedFactory("Grimholt-Chronos-Worker-");
        this.workers = new ThreadPoolExecutor(
                workerCount, workerCount, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity), workerFactory,
                new ThreadPoolExecutor.AbortPolicy());
        this.clock = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(
                namedFactory("Grimholt-Chronos-Clock-"));
    }

    /** Uses the JVM's visible processor count, including container CPU limits when respected by the JVM. */
    public static ChronosRegionScheduler forAvailableProcessors() {
        int processors = Math.max(1, Runtime.getRuntime().availableProcessors());
        // A small bounded queue absorbs brief scheduling jitter without storing many stale tick jobs.
        int queueCapacity = Math.max(16, Math.min(4096, processors * 4));
        return new ChronosRegionScheduler(processors, queueCapacity);
    }

    public Registration register(String regionId, Runnable tickAction) {
        Objects.requireNonNull(regionId, "regionId");
        Objects.requireNonNull(tickAction, "tickAction");
        if (regionId.isBlank()) throw new IllegalArgumentException("regionId must not be blank");
        if (closed.get()) throw new IllegalStateException("Chronos is closed");

        RegionTask task = new RegionTask(regionId, tickAction);
        RegionTask existing = regions.putIfAbsent(regionId, task);
        if (existing != null) throw new IllegalArgumentException("Region already registered: " + regionId);
        if (closed.get() && regions.remove(regionId, task)) {
            throw new IllegalStateException("Chronos is closed");
        }
        return new Registration(task);
    }

    /** Starts a 20-TPS target clock. Missed work is never replayed as a catch-up burst. */
    public void start() {
        if (closed.get()) throw new IllegalStateException("Chronos is closed");
        if (!started.compareAndSet(false, true)) return;
        clock.scheduleAtFixedRate(this::dispatchEpoch, 0L, tickMillis, TimeUnit.MILLISECONDS);
    }

    /** Manual dispatch hook for deterministic tests and controlled server orchestration. */
    public void dispatchEpoch() {
        if (closed.get()) return;
        epochs.incrementAndGet();
        for (RegionTask region : regions.values()) {
            if (region.closed.get()) continue;
            if (!region.inFlight.compareAndSet(false, true)) {
                region.skippedBusy.incrementAndGet();
                skippedBusyTicks.increment();
                continue;
            }

            try {
                workers.execute(() -> runRegionTick(region));
                submittedTicks.increment();
            } catch (RejectedExecutionException saturated) {
                region.inFlight.set(false);
                region.rejected.incrementAndGet();
                rejectedTicks.increment();
            }
        }
    }

    private void runRegionTick(RegionTask region) {
        long startedAt = System.nanoTime();
        try {
            if (!closed.get() && !region.closed.get()) region.tickAction.run();
            completedTicks.increment();
            region.completed.incrementAndGet();
        } catch (Throwable failure) {
            failedTicks.increment();
            region.failures.incrementAndGet();
            try {
                region.failureHandler.accept(failure);
            } catch (Throwable ignored) {
                // A diagnostics callback must never kill a worker or leak inFlight.
            }
        } finally {
            long elapsed = Math.max(0L, System.nanoTime() - startedAt);
            totalTickNanos.add(elapsed);
            region.lastDurationNanos.set(elapsed);
            region.maxDurationNanos.accumulateAndGet(elapsed, Math::max);
            region.inFlight.set(false);
        }
    }

    public int workerCount() { return workers.getCorePoolSize(); }
    public int activeWorkers() { return workers.getActiveCount(); }
    public int queuedTasks() { return workers.getQueue().size(); }
    public int regionCount() { return regions.size(); }
    public long epoch() { return epochs.get(); }

    public Metrics metrics() {
        return new Metrics(epochs.get(), regions.size(), workerCount(), activeWorkers(),
                queuedTasks(), submittedTicks.sum(), completedTicks.sum(),
                skippedBusyTicks.sum(), rejectedTicks.sum(), failedTicks.sum(),
                totalTickNanos.sum());
    }

    public RegionMetrics metrics(String regionId) {
        RegionTask region = regions.get(regionId);
        if (region == null) return null;
        return new RegionMetrics(region.id, region.completed.get(), region.skippedBusy.get(),
                region.rejected.get(), region.failures.get(), region.lastDurationNanos.get(),
                region.maxDurationNanos.get(), region.inFlight.get());
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        regions.values().forEach(region -> region.closed.set(true));
        regions.clear();
        clock.shutdownNow();
        workers.shutdown();
        try {
            if (!workers.awaitTermination(5, TimeUnit.SECONDS)) workers.shutdownNow();
        } catch (InterruptedException interrupted) {
            workers.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static ThreadFactory namedFactory(String prefix) {
        AtomicLong sequence = new AtomicLong();
        return task -> {
            Thread thread = new Thread(task, prefix + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    public record Metrics(long epoch, int regions, int workers, int activeWorkers, int queuedTasks,
                          long submittedTicks, long completedTicks, long skippedBusyTicks,
                          long rejectedTicks, long failedTicks, long totalTickNanos) {}

    public record RegionMetrics(String regionId, long completedTicks, long skippedBusyTicks,
                                long rejectedTicks, long failedTicks, long lastDurationNanos,
                                long maxDurationNanos, boolean inFlight) {}

    public final class Registration implements AutoCloseable {
        private final RegionTask task;
        private final AtomicBoolean registrationClosed = new AtomicBoolean();

        private Registration(RegionTask task) { this.task = task; }

        public String regionId() { return task.id; }

        public void onFailure(java.util.function.Consumer<Throwable> handler) {
            task.failureHandler = Objects.requireNonNull(handler, "handler");
        }

        @Override
        public void close() {
            if (!registrationClosed.compareAndSet(false, true)) return;
            task.closed.set(true);
            regions.remove(task.id, task);
        }
    }

    private static final class RegionTask {
        private final String id;
        private final Runnable tickAction;
        private final AtomicBoolean inFlight = new AtomicBoolean();
        private final AtomicBoolean closed = new AtomicBoolean();
        private final AtomicLong completed = new AtomicLong();
        private final AtomicLong skippedBusy = new AtomicLong();
        private final AtomicLong rejected = new AtomicLong();
        private final AtomicLong failures = new AtomicLong();
        private final AtomicLong lastDurationNanos = new AtomicLong();
        private final AtomicLong maxDurationNanos = new AtomicLong();
        private volatile java.util.function.Consumer<Throwable> failureHandler = ignored -> {};

        private RegionTask(String id, Runnable tickAction) {
            this.id = id;
            this.tickAction = tickAction;
        }
    }
}
