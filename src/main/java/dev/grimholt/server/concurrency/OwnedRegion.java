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
 * <p>Minestom remains the execution substrate. Grimholt owns the logical
 * ownership contract and uses this boundary to make cross-owner handoffs
 * explicit instead of allowing arbitrary threads to mutate region state.</p>
 */
public final class OwnedRegion implements AutoCloseable {
    private final RegionKey key;
    private final TickOwnership ownership = new TickOwnership();
    private final Queue<Runnable> handoffs;
    private final int maxHandoffs;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean drainScheduled = new AtomicBoolean();
    private final AtomicInteger failureCount = new AtomicInteger();
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
    public int pendingHandoffs() { synchronized (handoffs) { return handoffs.size(); } }
    public int failureCount() { return failureCount.get(); }

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
                try {
                    task.run();
                } catch (Throwable failure) {
                    failureCount.incrementAndGet();
                    try {
                        failureHandler.accept(failure);
                    } catch (Throwable handlerFailure) {
                        failureCount.incrementAndGet();
                    }
                }
            }
        });
        drainScheduled.set(false);
        boolean reschedule;
        synchronized (handoffs) {
            reschedule = !handoffs.isEmpty() && !closed.get();
        }
        if (reschedule) scheduleDrain();
    }

    private void scheduleDrain() {
        if (!drainScheduled.compareAndSet(false, true)) return;
        try {
            nextTickExecutor.accept(this::tick);
        } catch (RuntimeException | Error failure) {
            drainScheduled.set(false);
            synchronized (handoffs) { handoffs.clear(); }
            throw failure;
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            synchronized (handoffs) { handoffs.clear(); }
            drainScheduled.set(false);
        }
    }
}
