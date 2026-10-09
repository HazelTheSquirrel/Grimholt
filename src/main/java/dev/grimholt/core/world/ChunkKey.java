package dev.grimholt.core.world;

/**
 * A world chunk coordinate stored in one primitive-friendly 64-bit value.
 *
 * <p>The high 32 bits contain X and the low 32 bits contain Z. Unlike protocol-specific
 * position encodings, this representation preserves every signed {@code int} coordinate.
 * Hot-path collections can store {@link #packed()} directly as a primitive long key.</p>
 *
 * @param packed packed X/Z coordinate pair
 */
public record ChunkKey(long packed) {

    public static ChunkKey of(int x, int z) {
        // Widen before shifting so signed X occupies the high half without losing its bits.
        return new ChunkKey(((long) x << Integer.SIZE) | (z & 0xffff_ffffL));
    }

    public int x() {
        return (int) (packed >> Integer.SIZE);
    }

    public int z() {
        return (int) packed;
    }
}
