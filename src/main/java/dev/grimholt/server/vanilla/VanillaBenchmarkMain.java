package dev.grimholt.server.vanilla;

import java.util.Locale;

/**
 * Command-line microbenchmark for the Grimholt-owned world model.
 *
 * <p>This is intentionally not presented as a 500-1000 player network benchmark.
 * It measures concurrent region-local block read/write throughput and is useful
 * for regression detection while the network/world implementation is still under
 * construction.</p>
 */
public final class VanillaBenchmarkMain {
    private VanillaBenchmarkMain() {}

    public static void main(String[] args) throws Exception {
        int players = args.length > 0 ? Integer.parseInt(args[0]) : 1000;
        int regions = args.length > 1 ? Integer.parseInt(args[1]) : 50;
        int iterations = args.length > 2 ? Integer.parseInt(args[2]) : 20;

        VanillaMultithreadingBenchmark benchmark = new VanillaMultithreadingBenchmark();

        // Warm-up to reduce first-run JVM/JIT noise.
        benchmark.run(Math.min(players, 100), Math.max(1, Math.min(regions, 10)));

        long totalOps = 0;
        long totalNanos = 0;
        for (int i = 0; i < iterations; i++) {
            VanillaMultithreadingBenchmark.Result result = benchmark.run(players, regions);
            totalOps += result.operations();
            totalNanos += result.elapsedNanos();
        }

        double seconds = totalNanos / 1_000_000_000d;
        double opsPerSecond = totalOps / seconds;

        System.out.printf(Locale.ROOT,
            "Grimholt microbenchmark: players=%d regions=%d iterations=%d ops=%d time=%.3fs throughput=%.0f ops/s%n",
            players, regions, iterations, totalOps, seconds, opsPerSecond);
    }
}
