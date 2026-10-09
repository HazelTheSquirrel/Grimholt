package dev.grimholt.server.vanilla;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Access layer for data generated from the exact 26.4 server jar.
 *
 * <p>No hand-written substitute is accepted. Data is loaded from either the
 * explicit GRIMHOLT_VANILLA_DATA directory or the classpath location produced
 * by the Gradle generation task.</p>
 */
public final class VanillaGeneratedData {
    public static final String ROOT = "vanilla/26.4";
    private final Path filesystemRoot;
    private final ClassLoader classLoader;

    public VanillaGeneratedData() {
        this(System.getenv("GRIMHOLT_VANILLA_DATA"));
    }

    public VanillaGeneratedData(String explicitRoot) {
        this.filesystemRoot = explicitRoot == null || explicitRoot.isBlank() ? null : Path.of(explicitRoot);
        this.classLoader = VanillaGeneratedData.class.getClassLoader();
    }

    public boolean available() {
        if (filesystemRoot != null && Files.isDirectory(filesystemRoot)) {
            return Files.isRegularFile(filesystemRoot.resolve("manifest.properties"))
                && Files.isRegularFile(filesystemRoot.resolve("reports/blocks.json"));
        }
        try (InputStream manifest = classLoader.getResourceAsStream(ROOT + "/manifest.properties");
             InputStream blocks = classLoader.getResourceAsStream(ROOT + "/reports/blocks.json")) {
            return manifest != null && blocks != null;
        } catch (IOException e) {
            return false;
        }
    }

    public void requireAvailable() {
        if (!available()) {
            throw new IllegalStateException(
                "Exact 26.4 generated data is missing. Run generateVanilla26_4 " +
                "or set GRIMHOLT_VANILLA_DATA to its generated directory.");
        }
    }

    public Optional<String> read(String relativePath) {
        Objects.requireNonNull(relativePath, "relativePath");
        if (filesystemRoot != null) {
            Path p = filesystemRoot.resolve(relativePath).normalize();
            if (!p.startsWith(filesystemRoot.normalize())) throw new IllegalArgumentException("Path escapes data root");
            try {
                if (Files.isRegularFile(p)) return Optional.of(Files.readString(p, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read generated data: " + p, e);
            }
        }
        try (InputStream in = classLoader.getResourceAsStream(ROOT + "/" + relativePath)) {
            if (in == null) return Optional.empty();
            return Optional.of(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read generated resource: " + relativePath, e);
        }
    }

    public String require(String relativePath) {
        return read(relativePath).orElseThrow(() ->
            new IllegalStateException("Missing exact 26.4 generated file: " + relativePath));
    }

    public List<String> files() {
        if (filesystemRoot != null && Files.isDirectory(filesystemRoot)) {
            try (Stream<Path> stream = Files.walk(filesystemRoot)) {
                return stream.filter(Files::isRegularFile)
                    .map(filesystemRoot::relativize)
                    .map(Path::toString)
                    .sorted()
                    .toList();
            } catch (IOException e) {
                throw new IllegalStateException("Cannot enumerate generated data", e);
            }
        }
        return List.of();
    }
}
