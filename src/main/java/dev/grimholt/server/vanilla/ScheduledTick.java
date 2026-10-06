package dev.grimholt.server.vanilla;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public record ScheduledTick<T>(long dueTick, long sequence, T value) implements Comparable<ScheduledTick<T>> {
    private static final AtomicLong SEQUENCES = new AtomicLong();

    public ScheduledTick {
        if (dueTick < 0) throw new IllegalArgumentException("dueTick must be non-negative");
        Objects.requireNonNull(value, "value");
    }

    public static <T> ScheduledTick<T> at(long dueTick, T value) {
        return new ScheduledTick<>(dueTick, SEQUENCES.getAndIncrement(), value);
    }

    @Override
    public int compareTo(ScheduledTick<T> other) {
        int byTime = Long.compare(dueTick, other.dueTick);
        return byTime != 0 ? byTime : Long.compare(sequence, other.sequence);
    }
}
