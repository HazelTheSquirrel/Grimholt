package dev.grimholt.server.persistence;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public final class AtomicFileStore {
    private final Path root;

    public AtomicFileStore(Path root) {
        this.root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
    }

    public Path root() {
        return root;
    }

    public void write(String relative, byte[] data) throws IOException {
        Objects.requireNonNull(data, "data");
        Path target = resolve(relative);
        Path parent = target.getParent();
        rejectSymlinkParents(parent);
        Files.createDirectories(parent);
        rejectSymlinkParents(parent);

        // Unique same-directory temporary files avoid writers corrupting one
        // another when saves overlap. Same-directory moves preserve atomicity
        // on file systems that support ATOMIC_MOVE.
        String prefix = target.getFileName().toString();
        if (prefix.length() < 3) prefix = (prefix + "___").substring(0, 3);
        Path temporary = Files.createTempFile(parent, prefix, ".tmp");
        try {
            Files.write(temporary, data);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public byte[] read(String relative) throws IOException {
        Path target = resolve(relative);
        rejectSymlinkParents(target.getParent());
        if (Files.isSymbolicLink(target)) {
            throw new SecurityException("Persistence target must not be a symbolic link");
        }
        return Files.readAllBytes(target);
    }

    private Path resolve(String relative) {
        Objects.requireNonNull(relative, "relative");
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root) || target.equals(root)) {
            throw new SecurityException("Path escapes persistence root or names the root itself");
        }
        return target;
    }

    private void rejectSymlinkParents(Path parent) throws IOException {
        Path current = root;
        Path relativeParent = root.relativize(parent);
        if (Files.exists(root) && Files.isSymbolicLink(root)) {
            throw new SecurityException("Persistence root must not be a symbolic link");
        }
        for (Path component : relativeParent) {
            current = current.resolve(component);
            if (Files.exists(current) && Files.isSymbolicLink(current)) {
                throw new SecurityException("Persistence path contains a symbolic link: " + current);
            }
        }
    }
}
