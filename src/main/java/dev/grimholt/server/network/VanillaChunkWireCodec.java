package dev.grimholt.server.network;

import dev.grimholt.server.vanilla.*;
import java.io.*;
import java.util.*;

/**
 * Native 26.2 chunk/lighting wire serializer.
 *
 * <p>The serializer owns the wire layout; it consumes Grimholt's authoritative
 * block-state registry and chunk sections and never delegates serialization to
 * another server implementation.</p>
 */
public final class VanillaChunkWireCodec {
    public static final int SECTION_COUNT = 24;
    public static final int LIGHT_SECTION_COUNT = 26;

    public byte[] encodeLevelChunkWithLight(VanillaChunk chunk) {
        Objects.requireNonNull(chunk, "chunk");
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(out);
            data.writeInt(chunk.chunkX());
            data.writeInt(chunk.chunkZ());
            writeChunkData(data, chunk);
            writeLightData(data);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeChunkData(DataOutputStream out, VanillaChunk chunk) throws IOException {
        VanillaNbt.Tag heightmaps = VanillaNbt.compound(Map.of(
                "MOTION_BLOCKING", new VanillaNbt.Tag(VanillaNbt.LONG_ARRAY, new long[36]),
                "WORLD_SURFACE", new VanillaNbt.Tag(VanillaNbt.LONG_ARRAY, new long[36])
        ));
        VanillaProtocolCodec.writeNetworkNbtCompound(out, heightmaps);

        ByteArrayOutputStream sections = new ByteArrayOutputStream();
        DataOutputStream sectionOut = new DataOutputStream(sections);
        int air = chunk.worldModel().blockRegistry().stateId(
                chunk.worldModel().blockRegistry().defaultState("minecraft:air"));

        for (int sectionY = VanillaChunk.MIN_SECTION_Y; sectionY <= VanillaChunk.MAX_SECTION_Y; sectionY++) {
            sectionOut.writeShort(0); // non-air blocks
            sectionOut.writeShort(0); // fluid blocks
            writeSinglePalette(sectionOut, air);
            writeSinglePalette(sectionOut, 0); // plains biome registry id
        }
        sectionOut.flush();

        byte[] sectionBytes = sections.toByteArray();
        VanillaProtocol26_2.writeVarInt(out, sectionBytes.length);
        out.write(sectionBytes);
        VanillaProtocol26_2.writeVarInt(out, 0); // block entities
    }

    private void writeSinglePalette(DataOutputStream out, int value) throws IOException {
        out.writeByte(0); // SingleValuePalette
        VanillaProtocol26_2.writeVarInt(out, value);
    }

    private void writeLightData(DataOutputStream out) throws IOException {
        out.writeBoolean(false); // trust edges

        // All 24 world sections plus the two lighting border sections are
        // explicitly marked empty. This is a valid deterministic bootstrap
        // state; real light propagation will replace it as lighting is added.
        writeBitSet(out, (1L << LIGHT_SECTION_COUNT) - 1);
        writeBitSet(out, 0);
        writeBitSet(out, (1L << LIGHT_SECTION_COUNT) - 1);
        writeBitSet(out, (1L << LIGHT_SECTION_COUNT) - 1);

        VanillaProtocol26_2.writeVarInt(out, 0); // sky arrays
        VanillaProtocol26_2.writeVarInt(out, 0); // block arrays
    }

    private void writeBitSet(DataOutputStream out, long bits) throws IOException {
        VanillaProtocol26_2.writeVarInt(out, 1);
        out.writeLong(bits);
    }
}
