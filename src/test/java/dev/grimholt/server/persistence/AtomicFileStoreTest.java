package dev.grimholt.server.persistence;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AtomicFileStoreTest {
    @Test
    void roundTripReplacementAndTraversalProtection() throws Exception {
        Path dir = Files.createTempDirectory("grimholt-store");
        AtomicFileStore store = new AtomicFileStore(dir);

        store.write("state.bin", new byte[] {1, 2, 3});
        assertArrayEquals(new byte[] {1, 2, 3}, store.read("state.bin"));

        store.write("state.bin", new byte[] {4, 5});
        assertArrayEquals(new byte[] {4, 5}, store.read("state.bin"));
        assertThrows(SecurityException.class, () -> store.write("../escape", new byte[] {1}));
        assertThrows(SecurityException.class, () -> store.read("../escape"));
        assertThrows(SecurityException.class, () -> store.write(".", new byte[] {1}));
    }

    @Test
    void rejectsSymbolicLinkDirectoryWhenSupported() throws Exception {
        Path root = Files.createTempDirectory("grimholt-store-root");
        Path outside = Files.createTempDirectory("grimholt-store-outside");
        Path link = root.resolve("external");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException | java.io.IOException | SecurityException unsupported) {
            return; // The platform or CI user does not permit symbolic links.
        }

        AtomicFileStore store = new AtomicFileStore(root);
        assertThrows(SecurityException.class, () -> store.write("external/state.bin", new byte[] {1}));
        assertThrows(SecurityException.class, () -> store.read("external/state.bin"));
        assertFalse(Files.exists(outside.resolve("state.bin")));
    }
}
