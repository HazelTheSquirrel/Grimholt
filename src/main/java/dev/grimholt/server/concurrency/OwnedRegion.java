package dev.grimholt.server.concurrency;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final Consumer<Runnable> nextTickExecutor;

    public OwnedRegion(RegionKey key, int maxHandoffs, Consumer<Runnable> nextTickExecutor) {
        this.key = Objects.requireNonNull(key, "key");
        if (maxHandoffs < 1) throw new IllegalArgumentException("maxHandoffs must be positive");
        this.maxHandoffs = maxHandoffs;
        this.handoffs = new ArrayDeque<>(maxHandoffs);
        this.nextTickExecutor = Objects.requireNonNull(nextTickExecutor, "nextTickExecutor");
    }

    public RegionKey key() { return key; }
    public boolean closed() { return closed.get(); }
    public boolean ownedByCurrentThread() { return ownership.isOwnedByCurrentThread(); }

    /**
     * Executes immediately only when already inside this region's ownership
     * scope. Otherwise the operation is handed off to the region's next tick.
     */
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

    /**
     * Called by the region tick owner. All queued cross-owner work is executed
     * under the same ownership scope, then the queue is re-checked before the
     * scope is released.
     */
    public void tick() {
        if (closed.get()) return;
        ownership.run(() -> {
            for (;;) {
                Runnable task;
                synchronized (handoffs) {
                    task = handoffs.poll();
                }
                if (task == null) break;
                task.run();
            }
        });
    }

    private void scheduleDrain() {
        try {
            nextTickExecutor.accept(this::tick);
        } catch (RuntimeException failure) {
            synchronized (handoffs) {
                handoffs.clear();
            }
            throw failure;
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            synchronized (handoffs) {
                handoffs.clear();
            }
        }
    }
}
