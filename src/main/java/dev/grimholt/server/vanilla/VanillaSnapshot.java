package dev.grimholt.server.vanilla;

/**
 * Single source of truth for the currently targeted vanilla behavior reference.
 */
public final class VanillaSnapshot {
    public static final String VERSION = "26.2";
    public static final String MOJANG_VERSION_ID = VanillaSnapshot26_2.VERSION;
    public static final int PROTOCOL = VanillaSnapshot26_2.PROTOCOL;
    public static final int WORLD_DATA_VERSION = VanillaSnapshot26_2.WORLD_DATA_VERSION;
    public static final int DATA_PACK_VERSION = VanillaSnapshot26_2.DATA_PACK_VERSION;
    public static final int RESOURCE_PACK_VERSION = VanillaSnapshot26_2.RESOURCE_PACK_VERSION;

    private VanillaSnapshot() {}
}
