package dev.grimholt.server.vanilla;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fixed-size inventory model. Callers must provide ownership externally.
 */
public final class VanillaInventory {
    private final List<VanillaItemStack> slots;

    public VanillaInventory(int size) {
        if (size < 0) throw new IllegalArgumentException("size must be non-negative");
        slots = new ArrayList<>(size);
        for (int i = 0; i < size; i++) slots.add(VanillaItemStack.empty());
    }

    public int size() { return slots.size(); }

    public VanillaItemStack get(int slot) {
        checkSlot(slot);
        return slots.get(slot);
    }

    public void set(int slot, VanillaItemStack stack) {
        checkSlot(slot);
        slots.set(slot, Objects.requireNonNull(stack, "stack"));
    }

    public List<VanillaItemStack> snapshot() {
        return List.copyOf(slots);
    }

    public int firstEmpty() {
        for (int i = 0; i < slots.size(); i++) if (slots.get(i).isEmpty()) return i;
        return -1;
    }

    private void checkSlot(int slot) {
        if (slot < 0 || slot >= slots.size()) throw new IndexOutOfBoundsException("slot=" + slot);
    }
}
