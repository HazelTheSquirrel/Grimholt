package dev.grimholt.server.vanilla;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Stable state fingerprint used by differential tests.
 *
 * <p>Only canonical Grimholt state is included. This makes it possible to
 * compare the same scenario against the Mojang reference without pretending
 * that process exit or log output proves parity.</p>
 */
public final class VanillaStateFingerprint {
    private VanillaStateFingerprint() {}

    public static String world(VanillaWorldModel world) {
        MessageDigest digest = sha256();
        update(digest, "world=" + world.worldId());
        update(digest, "seed=" + world.seed());
        update(digest, "dimension=" + world.dimension());
        for (VanillaChunk chunk : world.chunks().stream()
                .sorted(java.util.Comparator.comparingInt(VanillaChunk::chunkX)
                        .thenComparingInt(VanillaChunk::chunkZ)).toList()) {
            update(digest, "chunk=" + chunk.chunkX() + "," + chunk.chunkZ());
            Map<BlockPos,BlockState> blocks = new TreeMap<>(
                    java.util.Comparator.comparingInt(BlockPos::x)
                            .thenComparingInt(BlockPos::y)
                            .thenComparingInt(BlockPos::z));
            blocks.putAll(chunk.blockSnapshot());
            for (var entry : blocks.entrySet()) {
                update(digest, entry.getKey() + "=" + entry.getValue().id() + ":" + new TreeMap<>(entry.getValue().properties()));
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    public static String player(VanillaPlayerState player) {
        MessageDigest digest = sha256();
        update(digest, "uuid=" + player.uuid());
        update(digest, "mode=" + player.gameMode());
        update(digest, "health=" + player.health());
        update(digest, "hunger=" + player.hunger());
        update(digest, "saturation=" + player.saturation());
        update(digest, "xp=" + player.experience() + ":" + player.level());
        update(digest, "pos=" + player.x() + "," + player.y() + "," + player.z()
                + "," + player.yaw() + "," + player.pitch() + ":" + player.onGround());
        return HexFormat.of().formatHex(digest.digest());
    }

    public static String entity(VanillaEntityState entity) {
        MessageDigest digest = sha256();
        update(digest, "uuid=" + entity.uuid());
        update(digest, "type=" + entity.typeId());
        update(digest, "pos=" + entity.x() + "," + entity.y() + "," + entity.z());
        update(digest, "velocity=" + entity.velocityX() + "," + entity.velocityY() + "," + entity.velocityZ());
        update(digest, "health=" + entity.health());
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static void update(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }
}
