package dev.grimholt.server.compat;

import net.minestom.server.MinecraftServer;

public final class CompatibilityProvider {
    public static final String VANILLA_BEHAVIOR_REFERENCE = "26.4 Snapshot 3";

    private CompatibilityProvider() {}

    public static CompatibilityInfo current(String implementationVersion) {
        return new CompatibilityInfo(
                "26.2",
                MinecraftServer.PROTOCOL_VERSION,
                MinecraftServer.DATA_VERSION,
                implementationVersion
        );
    }

    public static String vanillaBehaviorReference() {
        return VANILLA_BEHAVIOR_REFERENCE;
    }
}
