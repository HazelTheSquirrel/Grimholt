package dev.grimholt.server.vanilla;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Exact target contract for Minecraft Java Edition 26.4 Snapshot 3.
 *
 * <p>The values are pinned to the released snapshot. Generated data is never
 * approximated: callers must provide data generated from the exact reference
 * jar or the runtime fails closed.</p>
 */
public final class VanillaSnapshot26_4S3 {
    public static final String VERSION = "26.4-snapshot-3";
    public static final int PROTOCOL = 1_073_742_165;
    public static final int WORLD_DATA_VERSION = 5_122;
    public static final int DATA_PACK_VERSION = 123;
    public static final int RESOURCE_PACK_VERSION = 100;
    public static final int JAVA_MAJOR = 25;
    public static final String SERVER_SHA1 = "2d89c95c030e635387448f332961074ce1adbb4b";

    private VanillaSnapshot26_4S3() {}

    public static Reference requireReference(Path jar) {
        return new Reference(Objects.requireNonNull(jar), VERSION, PROTOCOL, WORLD_DATA_VERSION);
    }

    public record Reference(Path jar, String version, int protocol, int worldDataVersion) {
        public Reference {
            Objects.requireNonNull(jar, "jar");
            if (!VERSION.equals(version) || protocol != PROTOCOL || worldDataVersion != WORLD_DATA_VERSION) {
                throw new IllegalArgumentException("Reference is not Minecraft 26.4 Snapshot 3");
            }
        }
    }
}
