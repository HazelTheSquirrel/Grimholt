package dev.grimholt.server.vanilla;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Runs the pinned Mojang 26.4-snapshot-3 reference server for differential tests.
 * The jar is never embedded into Grimholt.
 */
public final class VanillaReferenceRunner26_2 {
    private final Path jar;
    private final Path workingDirectory;

    public VanillaReferenceRunner26_2(Path jar, Path workingDirectory) {
        this.jar = VanillaSnapshot26_2.requireReference(jar).jar();
        verifySha1(this.jar);
        this.workingDirectory = Objects.requireNonNull(workingDirectory);
    }

    public Path jar() { return jar; }

    private static void verifySha1(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            var digest = java.security.MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[8192];
            for (int n; (n = in.read(buffer)) >= 0;) digest.update(buffer, 0, n);
            String actual = java.util.HexFormat.of().formatHex(digest.digest());
            if (!VanillaSnapshot26_2.SERVER_SHA1.equals(actual)) {
                throw new IllegalArgumentException(
                    "Reference jar SHA-1 mismatch: expected " +
                    VanillaSnapshot26_2.SERVER_SHA1 + ", got " + actual);
            }
        } catch (IOException | java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot verify 26.2 reference jar", e);
        }
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
