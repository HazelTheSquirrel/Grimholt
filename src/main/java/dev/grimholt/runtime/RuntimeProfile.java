package dev.grimholt.runtime;

/** Immutable snapshot of runtime capacity inputs and conservative worker defaults. */
public record RuntimeProfile(int availableProcessors, long maxHeapBytes, int backgroundWorkers) {
    public RuntimeProfile {
        if (availableProcessors < 1 || maxHeapBytes < 1 || backgroundWorkers < 1) {
            throw new IllegalArgumentException("Runtime capacity values must be positive");
        }
    }

    public static RuntimeProfile detect() {
        return from(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().maxMemory());
    }

    static RuntimeProfile from(int processors, long maxHeapBytes) {
        if (processors < 1 || maxHeapBytes < 1) {
            throw new IllegalArgumentException("Runtime capacity inputs must be positive");
        }
        // Reserve a logical processor when possible; cap defaults to avoid excess worker stacks.
        int workers = Math.max(1, Math.min(8, processors - 1));
        return new RuntimeProfile(processors, maxHeapBytes, workers);
    }
}
