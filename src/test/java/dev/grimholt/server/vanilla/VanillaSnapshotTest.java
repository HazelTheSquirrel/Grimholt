package dev.grimholt.server.vanilla;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VanillaSnapshotTest {
    @Test
    void snapshotReferenceIsExplicitAndStable() {
        assertEquals("26.2", VanillaSnapshot.VERSION);
        assertEquals("107.1", VanillaSnapshot.DATA_PACK_VERSION);
        assertEquals("88", VanillaSnapshot.RESOURCE_PACK_VERSION);
    }
}
