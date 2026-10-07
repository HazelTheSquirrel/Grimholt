package dev.grimholt.server.vanilla;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Runs the real Mojang 26.4 Snapshot 3 server supplied by the operator.
 * The jar is never embedded into Grimholt.
 */
public final class VanillaReferenceRunner26_4S3 {
    private final Path jar;
    private final Path workingDirectory;

    public VanillaReferenceRunner26_4S3(Path jar, Path workingDirectory) {
        this.jar = VanillaSnapshot26_4S3.requireReference(jar).jar();
        this.workingDirectory = Objects.requireNonNull(workingDirectory);
    }

    public Process start(String... arguments) throws IOException {
        Files.createDirectories(workingDirectory);
        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        command.add("-Xms512M");
        command.add("-Xmx2G");
        command.add("-jar");
        command.add(jar.toAbsolutePath().toString());
        command.addAll(List.of(arguments));
        return new ProcessBuilder(command)
            .directory(workingDirectory.toFile())
            .redirectErrorStream(false)
            .start();
    }

    public Result run(long timeoutSeconds, String... arguments) throws IOException, InterruptedException {
        Process process = start(arguments);
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        return new Result(process.exitValue(), finished, stdout, stderr);
    }

    private static String javaExecutable() {
        String executable = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
            ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable).toString();
    }

    public record Result(int exitCode, boolean completed, String stdout, String stderr) {}
}
