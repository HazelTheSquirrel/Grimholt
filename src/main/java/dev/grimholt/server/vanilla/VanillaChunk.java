package dev.grimholt.server.vanilla;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sparse logical chunk state. The containing region is the sole gameplay owner;
 * the concurrent maps only protect lifecycle snapshots and do not replace ownership.
 */
public final class VanillaChunk {
    public static final int MIN_SECTION_Y = -4;
    public static final int MAX_SECTION_Y = 19;

    private final int chunkX;
    private final int chunkZ;
    private final Map<BlockPos, BlockState> blocks = new ConcurrentHashMap<>();
    private final Map<BlockPos, VanillaFluidState> fluids = new ConcurrentHashMap<>();
    private volatile boolean loaded;

    public VanillaChunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public int chunkX() { return chunkX; }
    public int chunkZ() { return chunkZ; }
    public boolean loaded() { return loaded; }

    public void load() { loaded = true; }
    public void unload() { loaded = false; }

    public BlockState block(BlockPos pos) {
        Objects.requireNonNull(pos, "pos");
        return blocks.getOrDefault(pos, BlockState.of("minecraft:air"));
    }

    public void setBlock(BlockPos pos, BlockState state) {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(state, "state");
        blocks.put(pos, state);
    }

    public VanillaFluidState fluid(BlockPos pos) {
        Objects.requireNonNull(pos, "pos");
        return fluids.getOrDefault(pos, VanillaFluidState.empty());
    }

    public void setFluid(BlockPos pos, VanillaFluidState state) {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(state, "state");
        if (state.isEmpty()) fluids.remove(pos);
        else fluids.put(pos, state);
    }

    public Map<BlockPos, BlockState> blockSnapshot() { return Map.copyOf(blocks); }
    public Map<BlockPos, VanillaFluidState> fluidSnapshot() { return Map.copyOf(fluids); }
}
