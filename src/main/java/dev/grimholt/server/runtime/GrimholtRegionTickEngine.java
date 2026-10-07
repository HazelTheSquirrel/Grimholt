package dev.grimholt.server.runtime;

import dev.grimholt.server.vanilla.VanillaServerKernel;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class GrimholtRegionTickEngine implements AutoCloseable {
    private final VanillaServerKernel kernel;
    private final ExecutorService executor;
    private final ScheduledExecutorService clock;
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicLong tick = new AtomicLong();

    public GrimholtRegionTickEngine(VanillaServerKernel kernel, GrimholtResourceProfile profile) {
        this.kernel = Objects.requireNonNull(kernel);
        int workers = Math.max(1, profile.workerParallelism());
        this.executor = Executors.newFixedThreadPool(workers, r -> {
            Thread t = new Thread(r, "Grimholt-Region-" + tick.get());
            t.setDaemon(true);
            return t;
        });
        this.clock = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Grimholt-TickClock");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        if (!running.compareAndSet(false, true)) return;
        clock.scheduleAtFixedRate(this::tickOnce, 0, 50, TimeUnit.MILLISECONDS);
    }

    private void tickOnce() {
        if (!running.get()) return;
        long current = tick.incrementAndGet();
        try {
            executor.execute(() -> {
                if (kernel.running()) kernel.tick();
            });
        } catch (RejectedExecutionException ignored) {}
    }

    public long tick() { return tick.get(); }

    @Override public void close() {
        running.set(false);
        clock.shutdownNow();
        executor.shutdownNow();
    }
}
