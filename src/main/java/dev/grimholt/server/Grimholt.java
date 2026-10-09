package dev.grimholt.server;

import dev.grimholt.server.api.*;
import dev.grimholt.server.command.GrimholtCommandDispatcher;
import dev.grimholt.server.config.*;
import dev.grimholt.server.lifecycle.*;
import dev.grimholt.server.logging.Logging;
import dev.grimholt.server.runtime.*;
import dev.grimholt.server.network.GrimholtNetworkServer;
import dev.grimholt.server.plugin.PluginBoundary;
import dev.grimholt.server.metrics.MetricsRegistry;
import dev.grimholt.server.security.SecurityLimits;
import dev.grimholt.server.vanilla.VanillaServerKernel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public final class Grimholt {
    private static final UUID OVERWORLD_ID =
            UUID.nameUUIDFromBytes("minecraft:overworld".getBytes(StandardCharsets.UTF_8));

    private final Lifecycle lifecycle = new Lifecycle();
    private final GrimholtResourceProfile resources = GrimholtResourceProfile.detect();
    private final ConfigLoader configLoader = new ConfigLoader();
    private final DefaultEventBus events = new DefaultEventBus();
    private final DefaultServiceRegistry services = new DefaultServiceRegistry();
    private final BoundedScheduler scheduler = BoundedScheduler.automatic(1024);
    private final GrimholtCommandDispatcher commands = new GrimholtCommandDispatcher();
    private final GrimholtServerImpl api = new GrimholtServerImpl(lifecycle, events, scheduler, services, commands);
    private final MetricsRegistry metrics = new MetricsRegistry();
    private final SecurityLimits limits = SecurityLimits.defaults();
    private final VanillaServerKernel vanillaKernel =
            new VanillaServerKernel(1024, scheduler::run, failure -> Logging.failure(failure));
    private final GrimholtNetworkServer network = new GrimholtNetworkServer(api, commands, vanillaKernel);
    private final GrimholtRegionTickEngine regionTicks =
            new GrimholtRegionTickEngine(vanillaKernel, resources);
    private final PluginBoundary plugins = new PluginBoundary(api, scheduler, events, services);
    private GrimholtConfig config;

    public Grimholt() {
        services.register(MetricsRegistry.class, metrics);
        services.register(SecurityLimits.class, limits);
        services.register(GrimholtResourceProfile.class, resources);
        services.register(GrimholtCommandDispatcher.class, commands);
        api.attachPluginManager(plugins);
    }

    public static void main(String[] args) {
        Path configPath = args.length == 0 ? Path.of("grimholt.properties") : Path.of(args[0]);
        Grimholt server = new Grimholt();
        CountDownLatch stopped = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop();
            } finally {
                stopped.countDown();
            }
        }, "Grimholt-Shutdown"));
        server.start(configPath);
        // Region workers, the tick clock and the socket acceptor are daemon
        // threads. Keep the server process alive until an OS shutdown signal
        // invokes the hook; otherwise main() would return and stop a healthy
        // standalone server immediately after its ready message.
        try {
            stopped.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            server.stop();
        }
    }

    public synchronized void start(Path configPath) {
        if (!lifecycle.beginStart()) throw new IllegalStateException("Grimholt cannot start from state " + lifecycle.state());
        try {
            config = configLoader.load(configPath);
            api.configureLimits(config.maxPlayers());
            Logging.startup(config.bindAddress(), config.port());
            vanillaKernel.start();
            vanillaKernel.registerWorld(OVERWORLD_ID);
            api.addWorld(OVERWORLD_ID, "minecraft:overworld");
            plugins.start();
            plugins.discover(Path.of("plugins"));
            network.start(config);
            regionTicks.start();
            plugins.loadAll();
            plugins.enableAll();
            lifecycle.started();
            events.post(new dev.grimholt.server.event.ServerReadyEvent());
            Logging.started();
        } catch (Throwable failure) {
            lifecycle.failed();
            try { plugins.disableAll(); } catch (Throwable x) { failure.addSuppressed(x); }
            try { regionTicks.close(); } catch (Throwable x) { failure.addSuppressed(x); }
            try { network.close(); } catch (Throwable x) { failure.addSuppressed(x); }
            try { vanillaKernel.stop(); } catch (Throwable x) { failure.addSuppressed(x); }
            scheduler.close();
            Logging.failure(failure);
            throwUnchecked(failure);
        }
    }

    public synchronized void stop() {
        if (!lifecycle.beginStop()) {
            var s = lifecycle.state();
            if (s == LifecycleState.STOPPED || s == LifecycleState.NEW) return;
            if (s == LifecycleState.FAILED) {
                try { plugins.disableAll(); }
                finally {
                    try { regionTicks.close(); }
                    finally {
                        try { network.close(); }
                        finally { try { vanillaKernel.stop(); } finally { scheduler.close(); } }
                    }
                }
                return;
            }
            throw new IllegalStateException("Grimholt cannot stop from state " + s);
        }
        Logging.stopping();
        Throwable failure = null;
        Logging.shutdownStage("begin plugins");
        try { plugins.disableAll(); } catch (Throwable x) { failure = x; }
        Logging.shutdownStage("plugins");
        try { regionTicks.close(); } catch (Throwable x) { if (failure == null) failure = x; else failure.addSuppressed(x); }
        Logging.shutdownStage("region tick engine");
        try { network.close(); } catch (Throwable x) { if (failure == null) failure = x; else failure.addSuppressed(x); }
        Logging.shutdownStage("network");
        try { vanillaKernel.stop(); } catch (Throwable x) { if (failure == null) failure = x; else failure.addSuppressed(x); }
        Logging.shutdownStage("vanilla kernel");
        scheduler.close();
        Logging.shutdownStage("scheduler");
        lifecycle.stopped();
        Logging.stopped();
        if (failure != null) { Logging.failure(failure); throwUnchecked(failure); }
    }

    private static void throwUnchecked(Throwable f) {
        if (f instanceof RuntimeException e) throw e;
        if (f instanceof Error e) throw e;
        throw new IllegalStateException("Grimholt lifecycle operation failed", f);
    }

    int boundPort() { return network.boundPort(); }

    public LifecycleState state() { return lifecycle.state(); }
    public GrimholtConfig config() { return config; }
    public GrimholtServerImpl api() { return api; }
    public MetricsRegistry.Snapshot metrics() { return metrics.snapshot(); }
    public SecurityLimits limits() { return limits; }
    public GrimholtResourceProfile resources() { return resources; }
    public VanillaServerKernel vanillaKernel() { return vanillaKernel; }
    public GrimholtNetworkServer network() { return network; }
    public GrimholtRegionTickEngine regionTicks() { return regionTicks; }
}
