package dev.grimholt.server.vanilla;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Deterministic region-owned scheduled tick phase.
 *
 * <p>This is part of the Grimholt gameplay model, not Minestom. It is kept
 * separate from chunk storage so the same tick semantics can later drive the
 * native Grimholt network/runtime.</p>
 */
public final class VanillaTickEngine {
    public enum Phase { TIME, SCHEDULED_BLOCKS, RANDOM_TICKS, ENTITIES, PLAYERS, NETWORK }

    private final VanillaWorldState world;
    private final BlockTickScheduler<BlockPos> scheduledBlocks;
    private long tickCount;
    private Consumer<Phase> phaseObserver = phase -> {};
    private Consumer<BlockPos> scheduledBlockTickHandler = pos -> {};

    public VanillaTickEngine(VanillaWorldState world, int maxScheduledBlockTicks) {
        this.world = Objects.requireNonNull(world, "world");
        this.scheduledBlocks = new BlockTickScheduler<>(maxScheduledBlockTicks);
    }

    public void onPhase(Consumer<Phase> observer) {
        phaseObserver = Objects.requireNonNull(observer, "observer");
    }

    public void onScheduledBlockTick(Consumer<BlockPos> handler) {
        scheduledBlockTickHandler = Objects.requireNonNull(handler, "handler");
    }

    public long tickCount() { return tickCount; }
    public VanillaWorldState world() { return world; }
    public BlockTickScheduler<BlockPos> scheduledBlocks() { return scheduledBlocks; }

    public void tick() {
        phaseObserver.accept(Phase.TIME);
        world.time().advance();
        tickCount++;

        phaseObserver.accept(Phase.SCHEDULED_BLOCKS);
        scheduledBlocks.runDue(tickCount, scheduledBlockTickHandler);

        phaseObserver.accept(Phase.RANDOM_TICKS);
        phaseObserver.accept(Phase.ENTITIES);
        phaseObserver.accept(Phase.PLAYERS);
        phaseObserver.accept(Phase.NETWORK);
    }
}
