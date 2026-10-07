package dev.grimholt.server.vanilla;

/**
 * Single source of truth for the currently targeted vanilla behavior reference.
 */
public final class VanillaSnapshot {
    public static final String VERSION = "26.4 Snapshot 3";
    public static final String MOJANG_VERSION_ID = VanillaSnapshot26_4S3.VERSION;
    public static final int PROTOCOL = VanillaSnapshot26_4S3.PROTOCOL;
    public static final int WORLD_DATA_VERSION = VanillaSnapshot26_4S3.WORLD_DATA_VERSION;
    public static final int DATA_PACK_VERSION = VanillaSnapshot26_4S3.DATA_PACK_VERSION;
    public static final int RESOURCE_PACK_VERSION = VanillaSnapshot26_4S3.RESOURCE_PACK_VERSION;

    private VanillaSnapshot() {}
}
