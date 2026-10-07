package dev.grimholt.server.minestom;

import dev.grimholt.server.api.GrimholtServerImpl;
import dev.grimholt.server.api.MinestomPlayer;
import dev.grimholt.server.config.GrimholtConfig;
import dev.grimholt.server.event.*;
import dev.grimholt.server.metrics.MetricsRegistry;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.minestom.server.world.DimensionType;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class MinestomAdapter {
    private MinecraftServer server;
    private InstanceContainer overworld;
    private final AtomicInteger admittedPlayers = new AtomicInteger();
    private final java.util.Set<UUID> admitted = ConcurrentHashMap.newKeySet();

    public void start(GrimholtConfig config) { start(config, null); }

    public void start(GrimholtConfig config, GrimholtServerImpl api) {
        if (server != null) throw new IllegalStateException("Minestom adapter already initialized");
        configureRuntime(config);
        MinecraftServer initialized = MinecraftServer.init(config.onlineMode() ? new Auth.Online() : new Auth.Offline());
        server = initialized;
        try {
            MinecraftServer.setBrandName("Grimholt");
            if (api != null) {
                var metrics = api.services().require(MetricsRegistry.class);
                Path worldPath = Path.of(config.worldDirectory());
                overworld = MinecraftServer.getInstanceManager().createInstanceContainer(
                        new AnvilLoader(worldPath, DimensionType.OVERWORLD.key()));
                overworld.enableAutoChunkLoad(true);
                api.addWorld(overworld);
                var events = MinecraftServer.getGlobalEventHandler();
                events.addListener(AsyncPlayerConfigurationEvent.class, e -> {
                    if (!reserve(e.getPlayer().getUuid(), config.maxPlayers())) {
                        e.getPlayer().kick(net.kyori.adventure.text.Component.text("Server is full."));
                        return;
                    }
                    e.setSpawningInstance(overworld);
                    e.getPlayer().setRespawnPoint(new Pos(0, 64, 0));
                });
                events.addListener(PlayerSpawnEvent.class, e -> {
                    if (e.isFirstSpawn()) {
                        var p = new MinestomPlayer(e.getPlayer());
                        api.addPlayer(e.getPlayer()); metrics.joined(); api.events().post(new PlayerJoinEvent(p));
                    }
                });
                events.addListener(PlayerDisconnectEvent.class, e -> {
                    release(e.getPlayer().getUuid());
                    var p = new MinestomPlayer(e.getPlayer()); metrics.quit(); api.events().post(new PlayerQuitEvent(p)); api.removePlayer(e.getPlayer().getUuid());
                });
            }
            initialized.start(config.socketAddress());
            if (api != null) api.bindTickScheduler(task -> MinecraftServer.getSchedulerManager().scheduleNextTick(task));
        } catch (RuntimeException | Error failure) {
            overworld = null; server = null; admitted.clear(); admittedPlayers.set(0);
            try { MinecraftServer.stopCleanly(); } catch (RuntimeException | Error cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    private static void configureRuntime(GrimholtConfig config) {
        System.setProperty("minestom.dispatcher-threads", Integer.toString(config.dispatcherThreads()));
        System.setProperty("minestom.chunk-view-distance", Integer.toString(config.viewDistance()));
        System.setProperty("minestom.entity-view-distance", Integer.toString(config.viewDistance()));
        System.setProperty("minestom.packet-queue-size", "1000");
    }

    private boolean reserve(UUID uuid, int limit) {
        for (;;) {
            int current = admittedPlayers.get();
            if (current >= limit) return false;
            if (admittedPlayers.compareAndSet(current, current + 1)) { admitted.add(uuid); return true; }
        }
    }

    private void release(UUID uuid) {
        if (admitted.remove(uuid)) admittedPlayers.decrementAndGet();
    }

    public void stop() {
        if (server == null) return;
        try {
            if (overworld != null) { overworld.saveInstance().join(); overworld.saveChunksToStorage().join(); }
            MinecraftServer.stopCleanly();
        } finally { overworld = null; server = null; admitted.clear(); admittedPlayers.set(0); }
    }
    public UUID overworldId() { return overworld == null ? null : overworld.getUuid(); }

    public boolean isStarted() { return server != null && MinecraftServer.isStarted(); }
}
