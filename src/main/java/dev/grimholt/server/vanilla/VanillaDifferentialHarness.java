package dev.grimholt.server.vanilla;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Differential harness against a real Mojang 26.2 reference.
 *
 * <p>The harness fails closed when no exact reference jar is supplied. A
 * successful process launch is not treated as gameplay parity; callers must
 * compare explicit golden state/packet/persistence results.</p>
 */
public final class VanillaDifferentialHarness {
    public record Result(boolean executed, int exitCode, long durationMs, String stdout, String stderr) {}

    private final Path referenceJar;

    public VanillaDifferentialHarness(Path referenceJar) {
        this.referenceJar = VanillaSnapshot26_2.requireReference(referenceJar).jar();
    }

    public Result run(List<String> args, Path workDir, long timeoutSeconds)
            throws IOException, InterruptedException {
        if (!Files.isRegularFile(referenceJar)) {
            throw new FileNotFoundException(referenceJar.toString());
        }
        Files.createDirectories(workDir);
        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        command.add("-jar");
        command.add(referenceJar.toAbsolutePath().toString());
        command.addAll(args);
        long start = System.nanoTime();
        Process process = new ProcessBuilder(command)
            .directory(workDir.toFile())
            .redirectErrorStream(false)
            .start();
        boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        return new Result(true, completed ? process.exitValue() : -1, duration, stdout, stderr);
    }

    public Path referenceJar() {
        return referenceJar;
    }

    public static Path requireReferenceFromEnvironment() {
        String value = System.getenv("GRIMHOLT_MC_26_2_JAR");
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                "Set GRIMHOLT_MC_26_2_JAR to the real Minecraft 26.2 server jar");
        }
        return VanillaSnapshot26_2.requireReference(Path.of(value)).jar();
    }

    public static void requireReference(String property) {
        String value = System.getenv(property);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                "Set " + property + " to the real Minecraft 26.2 server jar");
        }
        VanillaSnapshot26_2.requireReference(Path.of(value));
    }

    private static String javaExecutable() {
        String executable = System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable).toString();
    }
}
