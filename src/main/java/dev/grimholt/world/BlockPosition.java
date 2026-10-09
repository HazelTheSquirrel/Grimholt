package dev.grimholt.world;

/** Immutable block coordinates with correct negative-coordinate chunk mapping. */
public record BlockPosition(int x, int y, int z) {
    public ChunkKey chunkKey() {
        // floorDiv, unlike truncating division, maps negative coordinates to their real chunk.
        return ChunkKey.of(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
    }
    public int localX() { return Math.floorMod(x, 16); }
    public int localZ() { return Math.floorMod(z, 16); }
}
