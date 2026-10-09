package dev.grimholt.server.runtime;

import dev.grimholt.server.vanilla.VanillaServerKernel;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lifecycle bridge for Grimholt's region tick runtime.
 *
 * <p>Production uses the Chronos constructor: region registrations drive ticks
 * directly, with no second global tick queue. The legacy constructor remains
 * for tests and transitional callers that use the externally clocked kernel.</p>
 */
public final class GrimholtRegionTickEngine implements AutoCloseable {
    private final VanillaServerKernel kernel;
    private final ChronosRegionScheduler chronos;
    private final ExecutorService legacyExecutor;
    private final ScheduledExecutorService legacyClock;
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicLong legacyTick = new AtomicLong();

    public GrimholtRegionTickEngine(VanillaServerKernel kernel, GrimholtResourceProfile profile) {
        this.kernel = Objects.requireNonNull(kernel, "kernel");
        Objects.requireNonNull(profile, "profile");
        this.chronos = null;
        int workers = Math.max(1, profile.workerParallelism());
        this.legacyExecutor = Executors.newFixedThreadPool(workers, r -> {
            Thread t = new Thread(r, "Grimholt-Region-" + legacyTick.get());
            t.setDaemon(true);
            return t;
        });
        this.legacyClock = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Grimholt-TickClock");
            t.setDaemon(true);
            return t;
        });
    }

    /** Production lifecycle bridge: Chronos owns the clock and region dispatch. */
    public GrimholtRegionTickEngine(VanillaServerKernel kernel,
                                    GrimholtResourceProfile profile,
                                    ChronosRegionScheduler chronos) {
        this.kernel = Objects.requireNonNull(kernel, "kernel");
        Objects.requireNonNull(profile, "profile");
        this.chronos = Objects.requireNonNull(chronos, "chronos");
        this.legacyExecutor = null;
        this.legacyClock = null;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) return;
        if (chronos != null) {
            kernel.startRegionTicks();
            return;
        }
        legacyClock.scheduleAtFixedRate(this::tickOnce, 0, 50, TimeUnit.MILLISECONDS);
    }

    private void tickOnce() {
        if (!running.get()) return;
        legacyTick.incrementAndGet();
        try {
            legacyExecutor.execute(() -> {
                if (kernel.running()) kernel.tick();
            });
        } catch (RejectedExecutionException ignored) {
            // Shutdown races are expected; the executor queue is bounded by lifecycle.
        }
    }

    /** Number of dispatched epochs, not a promise that every region met its deadline. */
    public long tick() {
        return chronos != null ? chronos.epoch() : legacyTick.get();
    }

    @Override
    public void close() {
        running.set(false);
        if (chronos != null) {
            chronos.close();
            return;
        }
        legacyClock.shutdownNow();
        legacyExecutor.shutdownNow();
    }
}
