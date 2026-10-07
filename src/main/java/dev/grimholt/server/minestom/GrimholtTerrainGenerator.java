package dev.grimholt.server.minestom;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;

/**
 * Temporary compatibility terrain for the Minestom transport.
 *
 * <p>The terrain algorithm is Grimholt-owned and deterministic. It is not
 * claimed to be vanilla world-generation parity; the exact 26.4 generator
 * will replace this stage once the generated vanilla worldgen data/codecs are
 * wired into the native world pipeline.</p>
 */
public final class GrimholtTerrainGenerator implements Generator {
    private final long seed;

    public GrimholtTerrainGenerator(long seed) {
        this.seed = seed;
    }

    @Override
    public void generate(GenerationUnit unit) {
        BlockVec start = unit.absoluteStart();
        BlockVec end = unit.absoluteEnd();
        var modifier = unit.modifier();

        for (int x = start.blockX(); x < end.blockX(); x++) {
            for (int z = start.blockZ(); z < end.blockZ(); z++) {
                int surface = surfaceY(x, z);
                for (int y = start.blockY(); y < end.blockY(); y++) {
                    Block block;
                    if (y > surface) block = Block.AIR;
                    else if (y == surface) block = Block.GRASS_BLOCK;
                    else if (y >= surface - 3) block = Block.DIRT;
                    else block = Block.STONE;
                    modifier.setBlock(x, y, z, block);
                }
            }
        }
    }

    private int surfaceY(int x, int z) {
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
