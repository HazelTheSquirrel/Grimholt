package dev.grimholt.server.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class GrimholtPlayerInventoryTest {
    @Test
    void startsWithFirstHotbarSlotSelected() {
        GrimholtPlayerInventory inventory = new GrimholtPlayerInventory();

        assertEquals(46, inventory.size());
        assertEquals(0, inventory.selectedHotbarSlot());
    }

    @Test
    void acceptsEveryHotbarSlot() {
        GrimholtPlayerInventory inventory = new GrimholtPlayerInventory();

        for (int slot = 0; slot < 9; slot++) {
            inventory.selectedHotbarSlot(slot);
            assertEquals(slot, inventory.selectedHotbarSlot());
        }
    }

    @Test
    void rejectsSlotsOutsideHotbar() {
        GrimholtPlayerInventory inventory = new GrimholtPlayerInventory();

        assertThrows(IllegalArgumentException.class, () -> inventory.selectedHotbarSlot(-1));
        assertThrows(IllegalArgumentException.class, () -> inventory.selectedHotbarSlot(9));
    }
}
