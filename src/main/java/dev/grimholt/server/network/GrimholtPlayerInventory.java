package dev.grimholt.server.network;

import java.util.Arrays;

/**
 * Authoritative player inventory state for the native 26.4 play protocol.
 *
 * <p>The wire codec deliberately keeps empty stacks as the canonical VarInt(0)
 * representation. Non-empty component stacks are added through the item-stack
 * component codec once their registry-bound component definitions are available.</p>
 */
public final class GrimholtPlayerInventory {
    public static final int SLOT_COUNT = 46;
    private final byte[] slots = new byte[SLOT_COUNT];
    private int selectedHotbarSlot;

    public GrimholtPlayerInventory() {
        Arrays.fill(slots, (byte) 0);
    }

    public int selectedHotbarSlot() {
        return selectedHotbarSlot;
    }

    public void selectedHotbarSlot(int slot) {
        if (slot < 0 || slot > 8) throw new IllegalArgumentException("Hotbar slot must be 0..8");
        selectedHotbarSlot = slot;
    }

    public byte[] encodeEmptyInventory() {
        return Arrays.copyOf(slots, slots.length);
    }

    public int size() {
        return SLOT_COUNT;
    }
}
