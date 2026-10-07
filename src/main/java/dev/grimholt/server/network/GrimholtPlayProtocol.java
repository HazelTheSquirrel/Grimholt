package dev.grimholt.server.network;

import dev.grimholt.api.Position;
import dev.grimholt.server.vanilla.VanillaProtocol26_2;
import dev.grimholt.server.vanilla.VanillaProtocolCodec;
import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Native 26.2 clientbound PLAY bootstrap and movement packet codecs. */
public final class GrimholtPlayProtocol {
    public static final int DEFAULT_VIEW_DISTANCE = 8;
    public static final int DEFAULT_SIMULATION_DISTANCE = 8;

    public record Bootstrap(int entityId, String dimension, int maxPlayers,
                            int viewDistance, int simulationDistance, long seed,
                            Position position, boolean onlineMode) {}

    public byte[] login(Bootstrap bootstrap) {
        Objects.requireNonNull(bootstrap, "bootstrap");
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(out);
            data.writeInt(bootstrap.entityId());
            data.writeBoolean(false);
            VanillaProtocol26_2.writeVarInt(data, 1);
            VanillaProtocolCodec.writeIdentifier(data, bootstrap.dimension());
            VanillaProtocol26_2.writeVarInt(data, bootstrap.maxPlayers());
            VanillaProtocol26_2.writeVarInt(data, bootstrap.viewDistance());
            VanillaProtocol26_2.writeVarInt(data, bootstrap.simulationDistance());
            data.writeBoolean(false);
            data.writeBoolean(true);
            data.writeBoolean(false);

            VanillaProtocol26_2.writeVarInt(data, 0); // dimension type: overworld
            VanillaProtocolCodec.writeIdentifier(data, bootstrap.dimension());
            data.writeLong(bootstrap.seed());
            data.writeByte(0);  // survival
            data.writeByte(-1); // no previous game mode
            data.writeBoolean(false);
            data.writeBoolean(false);
            data.writeBoolean(false); // no last-death location
            VanillaProtocol26_2.writeVarInt(data, 0); // portal cooldown
            VanillaProtocol26_2.writeVarInt(data, 63); // sea level
            data.writeBoolean(bootstrap.onlineMode());
            data.writeBoolean(false); // secure chat not enforced
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] startWaitingForChunks() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(13);
            new DataOutputStream(out).writeFloat(0.0f);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] synchronizePosition(int teleportId, Position position) {
        Objects.requireNonNull(position, "position");
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(out);
            VanillaProtocol26_2.writeVarInt(data, teleportId);
            data.writeDouble(position.x());
            data.writeDouble(position.y());
            data.writeDouble(position.z());
            data.writeDouble(0.0);
            data.writeDouble(0.0);
            data.writeDouble(0.0);
            data.writeFloat(position.yaw());
            data.writeFloat(position.pitch());
            data.writeByte(0); // all relative flags clear
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] defaultSpawn(Position position) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(out);
            data.writeLong(((long) Math.floor(position.x()) & 0x3ffffffL) << 38
                    | ((long) Math.floor(position.z()) & 0x3ffffffL) << 12
                    | ((long) Math.floor(position.y()) & 0xfffL));
            data.writeFloat(position.yaw());
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] playerInfoAdd(UUID uuid, String username) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(out);
            data.writeByte(0x01 | 0x04 | 0x08 | 0x10 | 0x40 | 0x80);
            VanillaProtocol26_2.writeVarInt(data, 1);
            VanillaProtocolCodec.writeUuid(data, uuid);
            VanillaProtocolCodec.writeString(data, username, 16);
            VanillaProtocol26_2.writeVarInt(data, 0); // properties
            VanillaProtocol26_2.writeVarInt(data, 0); // game mode survival
            data.writeBoolean(true); // listed
            VanillaProtocol26_2.writeVarInt(data, 0); // latency
            data.writeBoolean(false); // no display name
            VanillaProtocol26_2.writeVarInt(data, 0); // priority
            data.writeBoolean(true); // hat
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
