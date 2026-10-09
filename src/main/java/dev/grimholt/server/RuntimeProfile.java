package dev.grimholt.server;

/**
 * Hardware-derived limits for Grimholt-owned background work.
 *
 * This profile intentionally does not reconfigure Minestom internals. The queue capacity
 * is bounded to make memory usage predictable when downstream work cannot keep up.
 */
public record RuntimeProfile(
        int logicalProcessors,
        long maxHeapBytes,
        int backgroundParallelism,
        int maxQueuedTasks
) {
    private static final long GIB = 1L << 30;
    private static final int MAX_BACKGROUND_THREADS = 16;
    private static final int MIN_QUEUE_CAPACITY = 256;
    private static final int MAX_QUEUE_CAPACITY = 4_096;

    public RuntimeProfile {
        if (logicalProcessors < 1) {
            throw new IllegalArgumentException("logicalProcessors must be positive");
        }
        if (maxHeapBytes < 1) {
            throw new IllegalArgumentException("maxHeapBytes must be positive");
        }
        if (backgroundParallelism < 1 || backgroundParallelism > MAX_BACKGROUND_THREADS) {
            throw new IllegalArgumentException("backgroundParallelism out of range");
        }
        if (maxQueuedTasks < MIN_QUEUE_CAPACITY || maxQueuedTasks > MAX_QUEUE_CAPACITY) {
            throw new IllegalArgumentException("maxQueuedTasks out of range");
        }
    }

    public static RuntimeProfile detect() {
        return from(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().maxMemory());
    }

    static RuntimeProfile from(int processors, long maxHeapBytes) {
        if (processors < 1) {
            throw new IllegalArgumentException("processors must be positive");
        }
        if (maxHeapBytes < 1) {
            throw new IllegalArgumentException("maxHeapBytes must be positive");
        }

        // Reserve CPU headroom for Minestom, networking, GC, and the operating system.
        final int cpuBudget = Math.max(1, processors - 1);
        // Heap-derived limits prevent a high-core, low-memory host from over-queuing work.
        final long heapGiB = Math.max(1L, maxHeapBytes / GIB);
        final int heapBudget = (int) Math.min(Integer.MAX_VALUE, heapGiB * 2L);
        final int workers = Math.max(1, Math.min(MAX_BACKGROUND_THREADS, Math.min(cpuBudget, heapBudget)));

        // Queue capacity is proportional to workers, but has hard bounds to cap retained tasks.
        final int queue = Math.max(MIN_QUEUE_CAPACITY,
                Math.min(MAX_QUEUE_CAPACITY, workers * 64));
        return new RuntimeProfile(processors, maxHeapBytes, workers, queue);
    }
}
