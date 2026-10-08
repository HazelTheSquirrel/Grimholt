package dev.grimholt.server.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GrimholtResourceProfileTest {

    @Test
    void detectsAtLeastOneProcessor() {
        GrimholtResourceProfile profile = GrimholtResourceProfile.detect();

        assertTrue(profile.logicalProcessors() >= 1);
        assertTrue(profile.workerParallelism() >= 1);
    }

    @Test
    void memoryBudgetRetainsTwentyPercentReserve() {
        GrimholtResourceProfile profile = GrimholtResourceProfile.detect();

        if (profile.physicalMemoryDetected()) {
            assertEquals(
                    Math.round(profile.physicalMemoryBytes() * GrimholtResourceProfile.SERVER_MEMORY_FRACTION),
                    profile.serverMemoryBudgetBytes());
            assertTrue(profile.serverMemoryBudgetBytes() <= profile.physicalMemoryBytes());
        } else {
            assertEquals(
                    Math.round(profile.jvmMaxHeapBytes() * GrimholtResourceProfile.SERVER_MEMORY_FRACTION),
                    profile.serverMemoryBudgetBytes());
            assertTrue(profile.serverMemoryBudgetBytes() <= profile.jvmMaxHeapBytes());
        }
    }

    @Test
    void rejectsInvalidProfiles() {
        assertThrows(IllegalArgumentException.class,
                () -> new GrimholtResourceProfile(0, 1, 1, 1, true));
        assertThrows(IllegalArgumentException.class,
                () -> new GrimholtResourceProfile(1, 1, 2, 1, true));
        assertThrows(IllegalArgumentException.class,
                () -> new GrimholtResourceProfile(1, 0, 81, 100, false));
    }
}
