package dev.grimholt.server.network;

/**
 * Connection-owned inventory selection state.
 *
 * <p>This is intentionally a small server-side model: item persistence and
 * inventory click transactions are separate gameplay systems. The selected
 * hotbar slot is validated at the network boundary before it is echoed to
 * the client.</p>
 */
final class GrimholtPlayerInventory {
    /** Player inventory slots represented by the current protocol inventory view. */
    private static final int SLOT_COUNT = 46;
    private static final int HOTBAR_SLOT_COUNT = 9;

    private int selectedHotbarSlot;

    int size() {
        return SLOT_COUNT;
    }

    int selectedHotbarSlot() {
        return selectedHotbarSlot;
    }

    void selectedHotbarSlot(int slot) {
        if (slot < 0 || slot >= HOTBAR_SLOT_COUNT) {
            throw new IllegalArgumentException(
                    "Selected hotbar slot must be in range [0, "
                            + (HOTBAR_SLOT_COUNT - 1) + "]: " + slot);
        }
        selectedHotbarSlot = slot;
    }
}
