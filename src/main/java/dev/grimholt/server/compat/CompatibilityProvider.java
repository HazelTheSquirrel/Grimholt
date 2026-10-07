package dev.grimholt.server.compat;

import dev.grimholt.server.vanilla.VanillaSnapshot26_2;

public final class CompatibilityProvider {
    public static final String VANILLA_BEHAVIOR_REFERENCE = VanillaSnapshot26_2.VERSION;

    private CompatibilityProvider() {}

    public static CompatibilityInfo current(String implementationVersion) {
        return new CompatibilityInfo(
                VanillaSnapshot26_2.VERSION,
                VanillaSnapshot26_2.PROTOCOL,
                VanillaSnapshot26_2.WORLD_DATA_VERSION,
                implementationVersion
        );
    }

    public static String vanillaBehaviorReference() {
        return VANILLA_BEHAVIOR_REFERENCE;
    }
}
