package dev.grimholt.server.network;

import dev.grimholt.server.vanilla.*;
import java.io.*;
import java.util.Objects;

/** Native 26.2 entity spawn/despawn payloads for the Grimholt-owned entity model. */
public final class GrimholtEntityWire {
    public byte[] addEntity(VanillaWorldModel world, int entityId, VanillaEntityState entity) {
        Objects.requireNonNull(world); Objects.requireNonNull(entity);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(out);
            VanillaProtocol26_2.writeVarInt(data, entityId);
            VanillaProtocolCodec.writeUuid(data, entity.uuid());
            VanillaProtocol26_2.writeVarInt(data, world.entityProtocolIds().requireId(entity.typeId()));
            data.writeDouble(entity.x());
            data.writeDouble(entity.y());
            data.writeDouble(entity.z());
            data.writeByte(0); // zero LP velocity
            VanillaProtocolCodec.writeAngle(data, 0);
            VanillaProtocolCodec.writeAngle(data, 0);
            VanillaProtocolCodec.writeAngle(data, 0);
            VanillaProtocol26_2.writeVarInt(data, 0); // type-specific data
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] removeEntity(int entityId) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            VanillaProtocol26_2.writeVarInt(out, 1);
            VanillaProtocol26_2.writeVarInt(out, entityId);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
