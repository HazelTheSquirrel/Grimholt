package dev.grimholt.server.vanilla;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VanillaSnapshotTest {
    @Test
    void snapshotReferenceIsExplicitAndStable() {
        assertEquals("26.4 Snapshot 3", VanillaSnapshot.VERSION);
        assertEquals(123, VanillaSnapshot.DATA_PACK_VERSION);
        assertEquals(100, VanillaSnapshot.RESOURCE_PACK_VERSION);
    }
}
