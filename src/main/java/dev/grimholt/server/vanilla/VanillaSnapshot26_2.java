package dev.grimholt.server.vanilla;

import java.nio.file.Path;
import java.util.Objects;

/**
 * @deprecated Historical class name retained for source compatibility only.
 * The target is Minecraft 26.4-snapshot-3; new code must use {@link VanillaSnapshot}.
 */
@Deprecated(forRemoval = false)
public final class VanillaSnapshot26_2 {
    public static final String VERSION = VanillaSnapshot.VERSION;
    public static final int PROTOCOL = VanillaSnapshot.PROTOCOL;
    public static final int WORLD_DATA_VERSION = VanillaSnapshot.WORLD_DATA_VERSION;
    public static final String DATA_PACK_VERSION = VanillaSnapshot.DATA_PACK_VERSION;
    public static final String RESOURCE_PACK_VERSION = VanillaSnapshot.RESOURCE_PACK_VERSION;
    public static final int JAVA_MAJOR = VanillaSnapshot.JAVA_MAJOR;
    public static final String SERVER_SHA1 = VanillaSnapshot.SERVER_SHA1;
    public static final String SERVER_URL = VanillaSnapshot.SERVER_URL;

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
