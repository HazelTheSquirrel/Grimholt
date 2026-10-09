package dev.grimholt.world;

/** Two signed 32-bit chunk coordinates stored losslessly in one 64-bit primitive value. */
public record ChunkKey(long packed) {
    public static ChunkKey of(int x, int z) {
        // Widen before shifting; mask Z so its sign bit cannot overwrite X.
        return new ChunkKey(((long) x << Integer.SIZE) | (z & 0xffff_ffffL));
    }
    public int x() { return (int) (packed >> Integer.SIZE); }
    public int z() { return (int) packed; }
}
