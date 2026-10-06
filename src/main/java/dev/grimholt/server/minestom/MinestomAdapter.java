package dev.grimholt.server.minestom;

import dev.grimholt.server.config.GrimholtConfig;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;

public final class MinestomAdapter {
    private MinecraftServer server;

    public void start(GrimholtConfig config) {
        if (server != null) throw new IllegalStateException("Minestom adapter already initialized");
        MinecraftServer initialized = MinecraftServer.init(new Auth.Offline());
        server = initialized;
        try {
            initialized.start(config.socketAddress());
        } catch (RuntimeException | Error failure) {
            server = null;
            try { MinecraftServer.stopCleanly(); }
            catch (RuntimeException | Error cleanupFailure) { failure.addSuppressed(cleanupFailure); }
            throw failure;
        }
    }

    public void stop() {
        if (server == null) return;
        try { MinecraftServer.stopCleanly(); }
        finally { server = null; }
    }

    public boolean isStarted() { return server != null && MinecraftServer.isStarted(); }
}
