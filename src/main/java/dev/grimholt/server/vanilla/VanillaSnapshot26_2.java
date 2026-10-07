package dev.grimholt.server.vanilla;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Exact target contract for Minecraft Java Edition 26.4-snapshot-3.
 *
 * <p>The values are pinned to the Mojang server.jar supplied by the official
 * manifest/object URL. Generated data must come from that exact reference.</p>
 */
public final class VanillaSnapshot26_2 {
    public static final String VERSION = "26.4-snapshot-3";
    public static final int PROTOCOL = 1_073_742_165;
    public static final int WORLD_DATA_VERSION = 5_122;
    public static final String DATA_PACK_VERSION = "123.0";
    public static final String RESOURCE_PACK_VERSION = "100.0";
    public static final int JAVA_MAJOR = 25;
    public static final String SERVER_SHA1 = "2d89c95c030e635387448f332961074ce1adbb4b";
    public static final String SERVER_URL =
            "https://piston-data.mojang.com/v1/objects/2d89c95c030e635387448f332961074ce1adbb4b/server.jar";

    private VanillaSnapshot26_2() {}

    public static Reference requireReference(Path jar) {
        return new Reference(Objects.requireNonNull(jar), VERSION, PROTOCOL, WORLD_DATA_VERSION);
    }

    public record Reference(Path jar, String version, int protocol, int worldDataVersion) {
        public Reference {
            Objects.requireNonNull(jar, "jar");
            if (!VERSION.equals(version) || protocol != PROTOCOL || worldDataVersion != WORLD_DATA_VERSION) {
                throw new IllegalArgumentException("Reference is not Minecraft 26.4-snapshot-3");
            }
        }
    }
}
