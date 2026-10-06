package dev.grimholt.server;

import dev.grimholt.server.config.ConfigLoader;
import dev.grimholt.server.config.GrimholtConfig;
import dev.grimholt.server.lifecycle.Lifecycle;
import dev.grimholt.server.lifecycle.LifecycleState;
import dev.grimholt.server.logging.Logging;
import dev.grimholt.server.minestom.MinestomAdapter;
import dev.grimholt.server.plugin.PluginBoundary;

import java.nio.file.Path;

public final class Grimholt {
    private final Lifecycle lifecycle = new Lifecycle();
    private final ConfigLoader configLoader = new ConfigLoader();
    private final MinestomAdapter minestom = new MinestomAdapter();
    private final PluginBoundary plugins = new PluginBoundary();
    private GrimholtConfig config;

    public static void main(String[] args) {
        Path configPath = args.length == 0 ? Path.of("grimholt.properties") : Path.of(args[0]);
        Grimholt server = new Grimholt();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "Grimholt-Shutdown"));
        server.start(configPath);
    }

    public synchronized void start(Path configPath) {
        if (!lifecycle.beginStart()) throw new IllegalStateException("Grimholt cannot start from state " + lifecycle.state());
        try {
            config = configLoader.load(configPath);
            Logging.startup(config.bindAddress(), config.port());
            plugins.start();
            plugins.discover();
            minestom.start(config);
            lifecycle.started();
            Logging.started();
        } catch (Throwable failure) {
            lifecycle.failed();
            try { plugins.stop(); } catch (Throwable cleanupFailure) { failure.addSuppressed(cleanupFailure); }
            try { minestom.stop(); } catch (Throwable cleanupFailure) { failure.addSuppressed(cleanupFailure); }
            Logging.failure(failure);
            throwUnchecked(failure);
        }
    }

    public synchronized void stop() {
        if (!lifecycle.beginStop()) {
            LifecycleState state = lifecycle.state();
            if (state == LifecycleState.STOPPED || state == LifecycleState.NEW) return;
            if (state == LifecycleState.FAILED) {
                minestom.stop();
                plugins.stop();
                return;
            }
            throw new IllegalStateException("Grimholt cannot stop from state " + state);
        }
        Logging.stopping();
        Throwable failure = null;
        try { minestom.stop(); } catch (Throwable exception) { failure = exception; }
        try { plugins.stop(); }
        catch (Throwable exception) {
            if (failure == null) failure = exception; else failure.addSuppressed(exception);
        }
        lifecycle.stopped();
        Logging.stopped();
        if (failure != null) {
            Logging.failure(failure);
            throwUnchecked(failure);
        }
    }

    private static void throwUnchecked(Throwable failure) {
        if (failure instanceof RuntimeException exception) throw exception;
        if (failure instanceof Error error) throw error;
        throw new IllegalStateException("Grimholt lifecycle operation failed", failure);
    }

    public LifecycleState state() { return lifecycle.state(); }
    public GrimholtConfig config() { return config; }
}
