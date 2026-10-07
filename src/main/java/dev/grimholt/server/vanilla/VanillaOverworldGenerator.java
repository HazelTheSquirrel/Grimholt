package dev.grimholt.server.vanilla;

/**
 * Grimholt-owned deterministic overworld terrain stage.
 *
 * <p>This is the bootstrap generator used before the full 26.4 density,
 * aquifer, carver, feature and structure pipeline is implemented. Its output
 * is deliberately deterministic and isolated behind this class so the final
 * vanilla generator can replace the stage without touching transport code.</p>
 */
public final class VanillaOverworldGenerator {
    private final long seed;

    public VanillaOverworldGenerator(long seed) {
        this.seed = seed;
    }

    public int surfaceY(int x, int z) {
        long h = seed;
        h ^= (long) x * 0x9E3779B97F4A7C15L;
        h ^= (long) z * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;
        int variation = (int) Math.floorMod(h, 9L) - 4;
        return 64 + variation;
    }
}
