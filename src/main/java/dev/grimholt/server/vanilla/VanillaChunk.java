package dev.grimholt.server.vanilla;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Sparse logical chunk state.
 *
 * <p>The containing region is the sole gameplay owner. Mutable maps therefore
 * remain ordinary owner-thread state; snapshots are immutable and safe to hand
 * to asynchronous readers.</p>
 */
public final class VanillaChunk {
    public static final int MIN_SECTION_Y = -4;
    public static final int MAX_SECTION_Y = 19;

    private final int chunkX;
    private final int chunkZ;
    private final VanillaWorldState world = new VanillaWorldState();
    private final Map<BlockPos, VanillaFluidState> fluids = new HashMap<>();
    private final VanillaTickEngine ticker = new VanillaTickEngine(world, 4096);
    private boolean loaded;

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
        return world.getBlock(pos);
    }

    public void setBlock(BlockPos pos, BlockState state) {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(state, "state");
        world.setBlock(pos, state);
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

    public VanillaWorldState world() { return world; }
    public VanillaTickEngine ticker() { return ticker; }

    public void tick() {
        if (!loaded) return;
        ticker.tick();
    }

    public Map<BlockPos, BlockState> blockSnapshot() { return world.snapshotBlocks(); }
    public Map<BlockPos, VanillaFluidState> fluidSnapshot() { return Map.copyOf(fluids); }
}
