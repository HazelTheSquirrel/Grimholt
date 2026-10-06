package dev.grimholt.server.vanilla;

/**
 * Single source of truth for the currently targeted vanilla behavior reference.
 *
 * <p>This does not claim protocol compatibility. The protocol/runtime substrate
 * remains whatever Minestom version Grimholt consumes.</p>
 */
public final class VanillaSnapshot {
    public static final String VERSION = "26.4 Snapshot 3";
    public static final int DATA_PACK_VERSION = 123;
    public static final int RESOURCE_PACK_VERSION = 100;

    private VanillaSnapshot() {}
}
