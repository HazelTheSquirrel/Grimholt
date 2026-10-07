package dev.grimholt.server.vanilla;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Grimholt-owned Minecraft gameplay kernel.
 *
 * <p>This is deliberately independent of Minestom. Minestom supplies network,
 * packet and low-level instance primitives; this class owns the mutable
 * gameplay model and its region execution boundary.</p>
 */
public final class VanillaServerKernel implements AutoCloseable {
    private final VanillaRegionManager regions;
    private final Map<UUID, VanillaWorldModel> worlds = new ConcurrentHashMap<>();
    private final Map<UUID, VanillaPlayerState> players = new ConcurrentHashMap<>();
    private final Consumer<Throwable> failureHandler;
    private volatile boolean running;

    public VanillaServerKernel(int maxHandoffs,
                               Consumer<Runnable> scheduler,
                               Consumer<Throwable> failureHandler) {
        this.failureHandler = Objects.requireNonNull(failureHandler, "failureHandler");
        this.regions = new VanillaRegionManager(
                maxHandoffs,
                Objects.requireNonNull(scheduler, "scheduler"),
                failureHandler);
    }

    public synchronized void start() {
        if (running) throw new IllegalStateException("Vanilla kernel already running");
        running = true;
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;
        regions.close();
        worlds.clear();
        players.clear();
    }

    public boolean running() {
        return running;
    }

    public void registerWorld(UUID worldId) {
        requireRunning();
        worlds.putIfAbsent(Objects.requireNonNull(worldId, "worldId"),
                new VanillaWorldModel(worldId));
    }

    public VanillaWorldModel world(UUID worldId) {
        return worlds.get(worldId);
    }

    public VanillaPlayerState registerPlayer(UUID worldId, UUID playerId) {
        requireRunning();
        if (!worlds.containsKey(worldId)) throw new IllegalArgumentException("Unknown world: " + worldId);
        Objects.requireNonNull(playerId, "playerId");
        return players.computeIfAbsent(playerId, ignored -> new VanillaPlayerState(playerId));
    }

    public VanillaPlayerState player(UUID playerId) {
        return players.get(playerId);
    }

    public void updatePlayerPosition(UUID worldId, UUID playerId, double x, double y, double z,
                                     float yaw, float pitch, boolean onGround) {
        VanillaPlayerState player = registerPlayer(worldId, playerId);
        player.position(x, y, z, yaw, pitch, onGround);
    }

    public VanillaPlayerState removePlayer(UUID playerId) {
        return players.remove(playerId);
    }

    public int playerCount() {
        return players.size();
    }

    public VanillaRegionRuntime region(UUID worldId, int chunkX, int chunkZ) {
        requireRunning();
        if (!worlds.containsKey(worldId)) {
            throw new IllegalArgumentException("Unknown world: " + worldId);
        }
        return regions.region(worldId, worlds.get(worldId), chunkX, chunkZ);
    }

    public void execute(UUID worldId, int chunkX, int chunkZ, Runnable action) {
        requireRunning();
        try {
            regions.execute(worldId, chunkX, chunkZ, action);
        } catch (Throwable failure) {
            failureHandler.accept(failure);
            throw failure;
        }
    }

    public int worldCount() {
        return worlds.size();
    }

    public int regionCount() {
        return regions.regionCount();
    }

    /** Advances all currently live logical regions without owning Minestom state. */
    public void tick() {
        requireRunning();
        regions.tickAll();
    }

    private void requireRunning() {
        if (!running) throw new IllegalStateException("Vanilla kernel is not running");
    }

    @Override
    public void close() {
        stop();
    }
}
