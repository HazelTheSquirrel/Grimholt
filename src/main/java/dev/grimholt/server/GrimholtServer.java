package dev.grimholt.server;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.block.Block;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Minimal production-shaped Minestom bootstrap.
 *
 * All registries, world generation, and player configuration are prepared before the socket
 * starts accepting clients. Minestom retains ownership of game and network thread scheduling.
 */
public final class GrimholtServer {
    private static final Logger LOGGER = Logger.getLogger(GrimholtServer.class.getName());
    private static final Pos SPAWN = new Pos(0.5, 41, 0.5);

    private GrimholtServer() {
    }

    public static void main(String[] args) {
        final ServerConfig config;
        try {
            config = ServerConfig.load();
        } catch (RuntimeException invalidConfiguration) {
            LOGGER.log(Level.SEVERE, "Invalid Grimholt configuration", invalidConfiguration);
            throw invalidConfiguration;
        }

        final RuntimeProfile runtime = RuntimeProfile.detect();
        LOGGER.info(() -> "Starting Grimholt on Java " + Runtime.version()
                + "; CPUs=" + runtime.logicalProcessors()
                + "; maxHeapMiB=" + (runtime.maxHeapBytes() >>> 20)
                + "; backgroundParallelism=" + runtime.backgroundParallelism()
                + "; backgroundQueueCapacity=" + runtime.maxQueuedTasks());

        final MinecraftServer server = MinecraftServer.init();
        Runtime.getRuntime().addShutdownHook(
                Thread.ofPlatform().name("grimholt-shutdown").unstarted(MinecraftServer::stopCleanly)
        );

        final InstanceManager instances = MinecraftServer.getInstanceManager();
        final InstanceContainer overworld = instances.createInstanceContainer();

        // Keep generation deterministic and cheap while the gameplay terrain pipeline is built.
        // fillHeight uses an exclusive upper bound: stone below the surface, one grass layer above.
        overworld.setGenerator(unit -> {
            unit.modifier().fillHeight(0, 39, Block.STONE);
            unit.modifier().fillHeight(39, 40, Block.GRASS_BLOCK);
        });

        final GlobalEventHandler events = MinecraftServer.getGlobalEventHandler();
        events.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();
            event.setSpawningInstance(overworld);
            player.setRespawnPoint(SPAWN);
        });

        LOGGER.info(() -> "World and login handlers ready; binding " + config.host() + ":" + config.port());
        try {
            server.start(config.host(), config.port());
        } catch (RuntimeException startupFailure) {
            LOGGER.log(Level.SEVERE, "Grimholt failed to start", startupFailure);
            MinecraftServer.stopCleanly();
            throw startupFailure;
        }
    }
}
