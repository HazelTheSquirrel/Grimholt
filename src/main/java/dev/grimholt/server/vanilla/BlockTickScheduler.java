package dev.grimholt.server.vanilla;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Consumer;

public final class BlockTickScheduler<T> {
    private final PriorityQueue<ScheduledTick<T>> queue = new PriorityQueue<>();
    private final int maxPending;

    public BlockTickScheduler(int maxPending) {
        if (maxPending < 1) throw new IllegalArgumentException("maxPending must be positive");
        this.maxPending = maxPending;
    }

    public synchronized void schedule(long dueTick, T value) {
        if (queue.size() >= maxPending) throw new IllegalStateException("scheduled tick queue is full");
        queue.add(ScheduledTick.at(dueTick, value));
    }

    public synchronized int pending() { return queue.size(); }

    public void runDue(long currentTick, Consumer<T> consumer) {
        List<ScheduledTick<T>> due = new ArrayList<>();
        synchronized (this) {
            while (!queue.isEmpty() && queue.peek().dueTick() <= currentTick) due.add(queue.poll());
        }
        for (ScheduledTick<T> tick : due) consumer.accept(tick.value());
    }

    public synchronized void clear() { queue.clear(); }
}
