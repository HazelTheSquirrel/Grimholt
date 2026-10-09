package dev.grimholt.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeProfileTest {
    @Test
    void reservesCpuHeadroomAndBoundsWorkerCount() {
        final RuntimeProfile profile = RuntimeProfile.from(8, 8L << 30);

        assertEquals(7, profile.backgroundParallelism());
        assertTrue(profile.maxQueuedTasks() <= 4_096);
        assertTrue(profile.maxQueuedTasks() >= 256);
    }

    @Test
    void handlesSingleCoreAndSmallHeap() {
        final RuntimeProfile profile = RuntimeProfile.from(1, 256L << 20);

        assertEquals(1, profile.backgroundParallelism());
        assertEquals(256, profile.maxQueuedTasks());
    }

    @Test
    void rejectsInvalidHardwareValues() {
        assertThrows(IllegalArgumentException.class, () -> RuntimeProfile.from(0, 1024));
        assertThrows(IllegalArgumentException.class, () -> RuntimeProfile.from(4, 0));
    }
}
