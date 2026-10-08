package dev.grimholt.server.runtime;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;

/**
 * Immutable snapshot of host resources available to Grimholt.
 *
 * <p>The server memory budget is capped at 80% of physical memory when
 * available, retaining a 20% reserve for the operating system and other
 * processes. If physical memory cannot be detected, the same reserve is
 * applied to the JVM's configured maximum heap.</p>
 */
public record GrimholtResourceProfile(
        int logicalProcessors,
        long physicalMemoryBytes,
        long serverMemoryBudgetBytes,
        long jvmMaxHeapBytes,
        boolean physicalMemoryDetected) {

    public static final double SERVER_MEMORY_FRACTION = 0.80d;

    public GrimholtResourceProfile {
        if (logicalProcessors < 1) {
            throw new IllegalArgumentException("logicalProcessors must be >= 1");
        }
        if (physicalMemoryBytes < 0 || serverMemoryBudgetBytes < 0 || jvmMaxHeapBytes < 0) {
            throw new IllegalArgumentException("resource values must not be negative");
        }
        if (serverMemoryBudgetBytes > physicalMemoryBytes && physicalMemoryBytes > 0) {
            throw new IllegalArgumentException("serverMemoryBudgetBytes exceeds physical memory");
        }
        if (jvmMaxHeapBytes > 0 && serverMemoryBudgetBytes > jvmMaxHeapBytes && !physicalMemoryDetected) {
            throw new IllegalArgumentException("fallback server budget exceeds the JVM maximum heap");
        }
    }

    public static GrimholtResourceProfile detect() {
        int processors = Math.max(1, Runtime.getRuntime().availableProcessors());
        OperatingSystemMXBean os = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

        long physical = 0L;
        if (os != null) {
            try {
                physical = Math.max(0L, os.getTotalMemorySize());
            } catch (UnsupportedOperationException ignored) {
                // Fall back to the JVM heap below.
            }
        }

        long jvmMax = Math.max(0L, Runtime.getRuntime().maxMemory());
        long budget = physical > 0
                ? Math.round(physical * SERVER_MEMORY_FRACTION)
                : Math.round(jvmMax * SERVER_MEMORY_FRACTION);

        return new GrimholtResourceProfile(processors, physical, budget, jvmMax, physical > 0);
    }

    public int workerParallelism() {
        return logicalProcessors;
    }

    public boolean usesFallbackMemoryBudget() {
        return !physicalMemoryDetected;
    }
}
