package dev.grimholt.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuntimeProfileTest {
    @Test void recommendsBoundedWorkersForDifferentHostSizes() {
        assertEquals(1, RuntimeProfile.from(1, 1L).backgroundWorkers());
        assertEquals(3, RuntimeProfile.from(4, 1L).backgroundWorkers());
        assertEquals(8, RuntimeProfile.from(64, 1L).backgroundWorkers());
    }
    @Test void rejectsInvalidCapacityInputs() {
        assertThrows(IllegalArgumentException.class, () -> RuntimeProfile.from(0, 1L));
        assertThrows(IllegalArgumentException.class, () -> RuntimeProfile.from(1, 0L));
    }
}
