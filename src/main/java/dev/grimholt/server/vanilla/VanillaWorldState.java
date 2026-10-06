package dev.grimholt.server.vanilla;

import java.util.HashMap;
import java.util.Map;

public final class VanillaWorldState {
    private final Map<BlockPos, BlockState> blocks = new HashMap<>();
    private final WorldTime time = new WorldTime();

    public WorldTime time() { return time; }

    public BlockState getBlock(BlockPos pos) {
        return blocks.getOrDefault(pos, BlockState.of("minecraft:air"));
    }

    public void setBlock(BlockPos pos, BlockState state) {
        blocks.put(pos, state);
    }

    public int blockCount() { return blocks.size(); }

    public Map<BlockPos, BlockState> snapshotBlocks() {
        return Map.copyOf(blocks);
    }
}
