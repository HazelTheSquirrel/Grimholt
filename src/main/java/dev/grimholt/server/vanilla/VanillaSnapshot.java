package dev.grimholt.server.vanilla;

/** Single source of truth for the currently targeted vanilla behavior reference. */
public final class VanillaSnapshot {
    public static final String VERSION = VanillaSnapshot26_2.VERSION;
    public static final String MOJANG_VERSION_ID = VanillaSnapshot26_2.VERSION;
    public static final int PROTOCOL = VanillaSnapshot26_2.PROTOCOL;
    public static final int WORLD_DATA_VERSION = VanillaSnapshot26_2.WORLD_DATA_VERSION;
    public static final String DATA_PACK_VERSION = VanillaSnapshot26_2.DATA_PACK_VERSION;
    public static final String RESOURCE_PACK_VERSION = VanillaSnapshot26_2.RESOURCE_PACK_VERSION;
    public static final int JAVA_MAJOR = VanillaSnapshot26_2.JAVA_MAJOR;
    public static final String SERVER_SHA1 = VanillaSnapshot26_2.SERVER_SHA1;
    private VanillaSnapshot() {}
}