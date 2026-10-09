package dev.grimholt.runtime;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;

/** Bounded cross-thread mailbox whose commands are applied by exactly one owner thread. */
public final class RegionMailbox {
    private final ArrayBlockingQueue<Runnable> queue;
    private volatile Thread owner;

    public RegionMailbox(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        queue = new ArrayBlockingQueue<>(capacity);
    }

    /** Enqueue without executing on the submitting thread; false means queue saturation. */
    public boolean submit(Runnable command) {
        return queue.offer(Objects.requireNonNull(command, "command"));
    }

    /** Drain at most limit commands. The scheduler decides when to call this method. */
    public int drain(int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit must be positive");
        claimOwner();
        int processed = 0;
        Runnable command;
        while (processed < limit && (command = queue.poll()) != null) {
            command.run();
            processed++;
        }
        return processed;
    }

    public int queuedCommands() { return queue.size(); }

    private void claimOwner() {
        Thread current = Thread.currentThread();
        Thread currentOwner = owner;
        if (currentOwner == null) {
            synchronized (this) {
                if (owner == null) owner = current;
                currentOwner = owner;
            }
        }
        if (currentOwner != current) throw new IllegalStateException("Mailbox can only be drained by its owner thread");
    }
}
