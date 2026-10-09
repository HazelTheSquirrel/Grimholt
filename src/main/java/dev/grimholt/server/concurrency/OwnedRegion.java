package dev.grimholt.server.concurrency;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Bounded, single-owner execution boundary for mutable region state.
 *
 * <p>Grimholt owns the execution boundary. Cross-region handoffs are queued
 * and executed only by the region owner; no external server runtime owns this state.</p>
 */
public final class OwnedRegion implements AutoCloseable {
    private final RegionKey key;
    private final TickOwnership ownership = new TickOwnership();
    private final Queue<Runnable> handoffs;
    private final int maxHandoffs;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean drainScheduled = new AtomicBoolean();
    private final Object executionLock = new Object();
    private final AtomicInteger failureCount = new AtomicInteger();
    private final AtomicInteger drainSchedulingFailures = new AtomicInteger();
    private final Consumer<Runnable> nextTickExecutor;
    private final Consumer<Throwable> failureHandler;

    public OwnedRegion(RegionKey key, int maxHandoffs, Consumer<Runnable> nextTickExecutor) {
        this(key, maxHandoffs, nextTickExecutor, failure -> {});
    }

    public OwnedRegion(RegionKey key, int maxHandoffs, Consumer<Runnable> nextTickExecutor,
                       Consumer<Throwable> failureHandler) {
        this.key = Objects.requireNonNull(key, "key");
        if (maxHandoffs < 1) throw new IllegalArgumentException("maxHandoffs must be positive");
        this.maxHandoffs = maxHandoffs;
        this.handoffs = new ArrayDeque<>(maxHandoffs);
        this.nextTickExecutor = Objects.requireNonNull(nextTickExecutor, "nextTickExecutor");
        this.failureHandler = Objects.requireNonNull(failureHandler, "failureHandler");
    }

    public RegionKey key() { return key; }
    public boolean closed() { return closed.get(); }
    public boolean ownedByCurrentThread() { return ownership.isOwnedByCurrentThread(); }

    /** Fails fast when the current thread does not own this region. */
    public void assertOwner() { ownership.assertOwner(); }
    public int pendingHandoffs() { synchronized (handoffs) { return handoffs.size(); } }
    public int failureCount() { return failureCount.get(); }
    public int drainSchedulingFailures() { return drainSchedulingFailures.get(); }

    public void execute(Runnable action) {
        Objects.requireNonNull(action, "action");
        if (closed.get()) throw new RejectedExecutionException("Region is closed");
        if (ownership.isOwnedByCurrentThread()) {
            action.run();
            return;
        }
        synchronized (handoffs) {
            if (closed.get()) throw new RejectedExecutionException("Region is closed");
            if (handoffs.size() >= maxHandoffs) {
                throw new RejectedExecutionException("Region handoff queue is full: " + key);
            }
            handoffs.add(action);
        }
        scheduleDrain();
    }

    public void tick() {
        tick(() -> {});
    }

    /**
     * Drains queued cross-thread work and then runs the region simulation under
     * the same exclusive ownership scope. The lock serializes this path with
     * independently scheduled handoff drains; the logical owner remains the
     * region, not whichever worker happens to execute it.
     */
    public void tick(Runnable regionTick) {
        Objects.requireNonNull(regionTick, "regionTick");
        synchronized (executionLock) {
            if (closed.get()) {
                drainScheduled.set(false);
                return;
            }
            ownership.run(() -> {
                for (;;) {
                    Runnable task;
                    synchronized (handoffs) {
                        task = handoffs.poll();
                    }
                    if (task == null) break;
                    runGuarded(task);
                }
                runGuarded(regionTick);
            });
        }

        boolean reschedule;
        synchronized (handoffs) {
            reschedule = !handoffs.isEmpty() && !closed.get();
            if (!reschedule) drainScheduled.set(false);
        }
        if (reschedule) {
            try {
                nextTickExecutor.accept(this::tick);
            } catch (RuntimeException | Error failure) {
                // The action is already accepted. Preserve it so the next regular
                // region tick can drain it after transient worker-queue saturation.
                drainSchedulingFailures.incrementAndGet();
                drainScheduled.set(false);
            }
        }
    }

    private void runGuarded(Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            failureCount.incrementAndGet();
            try {
                failureHandler.accept(failure);
            } catch (Throwable handlerFailure) {
                failureCount.incrementAndGet();
            }
        }
    }

    private void scheduleDrain() {
        if (!drainScheduled.compareAndSet(false, true)) return;
        try {
            nextTickExecutor.accept(this::tick);
        } catch (RuntimeException | Error failure) {
            // Never discard accepted mutations on transient executor saturation.
            // Automatic Chronos ticks provide another drain opportunity.
            drainSchedulingFailures.incrementAndGet();
            drainScheduled.set(false);
        }
    }

    @Override
    public void close() {
        // Shutdown must not unload mutable region state concurrently with a tick.
        synchronized (executionLock) {
            if (closed.compareAndSet(false, true)) {
                synchronized (handoffs) { handoffs.clear(); }
                drainScheduled.set(false);
            }
        }
    }
}
