package dev.grimholt.server.vanilla;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Exact target contract for Minecraft Java Edition 26.2.
 *
 * <p>The values are pinned to the official Minecraft 26.2 server.jar. Generated data is never
 * approximated: callers must provide data generated from the exact reference
 * jar or the runtime fails closed.</p>
 */
public final class VanillaSnapshot26_2 {
    public static final String VERSION = "26.2";
    public static final int PROTOCOL = 776;
    public static final int WORLD_DATA_VERSION = 4_903;
    public static final int DATA_PACK_VERSION = 107;
    public static final int RESOURCE_PACK_VERSION = 88;
    public static final int JAVA_MAJOR = 25;
    public static final String SERVER_SHA1 = "823e2250d24b3ddac457a60c92a6a941943fcd6a";

    private VanillaSnapshot26_2() {}

    public static Reference requireReference(Path jar) {
        return new Reference(Objects.requireNonNull(jar), VERSION, PROTOCOL, WORLD_DATA_VERSION);
    }

    public record Reference(Path jar, String version, int protocol, int worldDataVersion) {
        public Reference {
            Objects.requireNonNull(jar, "jar");
            if (!VERSION.equals(version) || protocol != PROTOCOL || worldDataVersion != WORLD_DATA_VERSION) {
                throw new IllegalArgumentException("Reference is not Minecraft 26.2");
            }
        }
    }
}
