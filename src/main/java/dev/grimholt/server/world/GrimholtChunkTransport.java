package dev.grimholt.server.world;

import dev.grimholt.server.vanilla.*;
import java.util.*;

public final class GrimholtChunkTransport {
    public record ChunkSnapshot(int chunkX, int chunkZ, List<BlockState> blocks) {}

    public ChunkSnapshot snapshot(VanillaWorldModel world, int chunkX, int chunkZ) {
        Objects.requireNonNull(world, "world");
        VanillaChunk chunk = world.chunk(chunkX, chunkZ);
        List<BlockState> blocks = new ArrayList<>();
        for (int y = -64; y < 320; y++) {
            for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                blocks.add(chunk.block(new BlockPos(chunkX * 16 + x, y, chunkZ * 16 + z)));
            }
        }
        return new ChunkSnapshot(chunkX, chunkZ, List.copyOf(blocks));
    }
}
