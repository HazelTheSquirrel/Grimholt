package dev.grimholt.server.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GrimholtPlayerInventoryTest {
    @Test
    void startsEmptyWithFirstHotbarSlotSelected() {
        GrimholtPlayerInventory inventory = new GrimholtPlayerInventory();
        assertEquals(46, inventory.size());
        assertEquals(0, inventory.selectedHotbarSlot());
        assertEquals(46, inventory.encodeEmptyInventory().length);
    }

    @Test
    void validatesHotbarSelection() {
        GrimholtPlayerInventory inventory = new GrimholtPlayerInventory();
        inventory.selectedHotbarSlot(8);
        assertEquals(8, inventory.selectedHotbarSlot());
        assertThrows(IllegalArgumentException.class, () -> inventory.selectedHotbarSlot(-1));
        assertThrows(IllegalArgumentException.class, () -> inventory.selectedHotbarSlot(9));
    }
}
