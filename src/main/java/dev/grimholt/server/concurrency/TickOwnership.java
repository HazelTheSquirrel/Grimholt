package dev.grimholt.server.concurrency;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runtime guard for state that has exactly one active tick owner.
 *
 * <p>The owner is deliberately a thread for the duration of an ownership
 * scope, not permanently. This matches a regionized scheduler where the same
 * logical region may be ticked by different worker threads over time.</p>
 */
public final class TickOwnership {
    private final AtomicReference<Thread> owner = new AtomicReference<>();
    private int depth;

    public void enter() {
        Thread current = Thread.currentThread();
        synchronized (this) {
            Thread existing = owner.get();
            if (existing == null) {
                owner.set(current);
            } else if (existing != current) {
                throw new IllegalStateException(
                        "Tick ownership violation: owned by " + existing.getName()
                                + ", accessed by " + current.getName());
            }
            depth++;
        }
    }

    public void exit() {
        Thread current = Thread.currentThread();
        synchronized (this) {
            if (owner.get() != current || depth == 0) {
                throw new IllegalStateException("Tick ownership exit without ownership");
            }
            if (--depth == 0) {
                owner.set(null);
            }
        }
    }

    public void assertOwner() {
        Thread current = Thread.currentThread();
        Thread existing = owner.get();
        if (existing != current) {
            throw new IllegalStateException(
                    "Tick ownership violation: owned by "
                            + (existing == null ? "<none>" : existing.getName())
                            + ", accessed by " + current.getName());
        }
    }

    public boolean isOwnedByCurrentThread() {
        return owner.get() == Thread.currentThread();
    }

    public void run(Runnable action) {
        Objects.requireNonNull(action, "action");
        enter();
        try {
            action.run();
        } finally {
            exit();
        }
    }

    public <T> T call(Callable<T> action) throws Exception {
        Objects.requireNonNull(action, "action");
        enter();
        try {
            return action.call();
        } finally {
            exit();
        }
    }
}
