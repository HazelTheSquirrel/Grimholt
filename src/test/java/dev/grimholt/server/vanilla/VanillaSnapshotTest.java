package dev.grimholt.server.vanilla;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VanillaSnapshotTest {
    @Test void snapshotReferenceIsExplicitAndStable() {
        assertEquals("26.4-snapshot-3", VanillaSnapshot.VERSION);
        assertEquals(1_073_742_165, VanillaSnapshot.PROTOCOL);
        assertEquals("123.0", VanillaSnapshot.DATA_PACK_VERSION);
        assertEquals("100.0", VanillaSnapshot.RESOURCE_PACK_VERSION);
    }
}