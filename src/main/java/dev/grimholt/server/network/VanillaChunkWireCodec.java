package dev.grimholt.server.network;

import dev.grimholt.server.vanilla.*;
import java.io.*;
import java.util.*;

/** Native 26.2 chunk/lighting wire serializer owned by Grimholt. */
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
        VanillaNbt.Tag heightmaps = heightmaps(chunk);
        VanillaProtocolCodec.writeNetworkNbtCompound(out, heightmaps);

        ByteArrayOutputStream sections = new ByteArrayOutputStream();
        DataOutputStream sectionOut = new DataOutputStream(sections);
        int air = chunk.worldModel().blockRegistry().stateId(
                chunk.worldModel().blockRegistry().defaultState("minecraft:air"));

        for (int sectionY = VanillaChunk.MIN_SECTION_Y; sectionY <= VanillaChunk.MAX_SECTION_Y; sectionY++) {
            int nonAir = 0;
            int fluid = 0;
            int[] states = new int[4096];
            Map<Integer, Integer> paletteIndex = new LinkedHashMap<>();
            for (int ly = 0; ly < 16; ly++) {
                for (int lz = 0; lz < 16; lz++) {
                    for (int lx = 0; lx < 16; lx++) {
                        BlockPos pos = new BlockPos(chunk.chunkX() * 16 + lx, sectionY * 16 + ly,
                                chunk.chunkZ() * 16 + lz);
                        BlockState state = chunk.block(pos);
                        int id = chunk.worldModel().blockRegistry().stateId(state);
                        int index = paletteIndex.computeIfAbsent(id, ignored -> paletteIndex.size());
                        states[(ly << 8) | (lz << 4) | lx] = index;
                        if (id != air) nonAir++;
                        if (!chunk.fluid(pos).isEmpty()) fluid++;
                    }
                }
            }
            sectionOut.writeShort(nonAir);
            sectionOut.writeShort(fluid);
            writePalettedContainer(sectionOut, paletteIndex, states);
            writeSinglePalette(sectionOut, 0); // plains biome
        }
        sectionOut.flush();

        byte[] sectionBytes = sections.toByteArray();
        VanillaProtocol26_2.writeVarInt(out, sectionBytes.length);
        out.write(sectionBytes);
        VanillaProtocol26_2.writeVarInt(out, 0); // block entities
    }

    private VanillaNbt.Tag heightmaps(VanillaChunk chunk) {
        long[] surface = new long[36];
        long[] motion = new long[36];
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int height = 0;
                for (int y = VanillaChunk.MAX_SECTION_Y * 16 + 15; y >= VanillaChunk.MIN_SECTION_Y * 16; y--) {
                    if (!chunk.block(new BlockPos(chunk.chunkX() * 16 + x, y, chunk.chunkZ() * 16 + z))
                            .id().equals("minecraft:air")) {
                        height = y + 1;
                        break;
                    }
                }
                int value = Math.max(0, Math.min(511, height + 64));
                setPacked(surface, (z * 16 + x) * 9, value, 9);
                setPacked(motion, (z * 16 + x) * 9, value, 9);
            }
        }
        return VanillaNbt.compound(Map.of(
                "MOTION_BLOCKING", new VanillaNbt.Tag(VanillaNbt.LONG_ARRAY, motion),
                "WORLD_SURFACE", new VanillaNbt.Tag(VanillaNbt.LONG_ARRAY, surface)));
    }

    private static void setPacked(long[] data, int bitIndex, int value, int bits) {
        int word = bitIndex >>> 6;
        int offset = bitIndex & 63;
        data[word] |= ((long) value & ((1L << bits) - 1)) << offset;
        if (offset + bits > 64) {
            data[word + 1] |= ((long) value & ((1L << (offset + bits - 64)) - 1)) >>> (64 - offset);
        }
    }

    private void writePalettedContainer(DataOutputStream out, Map<Integer,Integer> palette, int[] values) throws IOException {
        if (palette.size() == 1) {
            writeSinglePalette(out, palette.keySet().iterator().next());
            return;
        }
        int bits = Math.max(4, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
        bits = Math.min(bits, 8);
        out.writeByte(bits);
        VanillaProtocol26_2.writeVarInt(out, palette.size());
        for (int globalId : palette.keySet()) VanillaProtocol26_2.writeVarInt(out, globalId);
        int valuesPerLong = 64 / bits;
        int longCount = (values.length + valuesPerLong - 1) / valuesPerLong;
        VanillaProtocol26_2.writeVarInt(out, longCount);
        for (int base = 0; base < values.length; base += valuesPerLong) {
            long packed = 0;
            for (int i = 0; i < valuesPerLong && base + i < values.length; i++)
                packed |= ((long) values[base + i]) << (i * bits);
            out.writeLong(packed);
        }
    }

    private void writeSinglePalette(DataOutputStream out, int value) throws IOException {
        out.writeByte(0);
        VanillaProtocol26_2.writeVarInt(out, value);
        VanillaProtocol26_2.writeVarInt(out, 0); // zero packed-data longs for a single-value palette
    }

    private void writeLightData(DataOutputStream out) throws IOException {
        out.writeBoolean(false);
        writeBitSet(out, (1L << LIGHT_SECTION_COUNT) - 1);
        writeBitSet(out, 0);
        writeBitSet(out, (1L << LIGHT_SECTION_COUNT) - 1);
        writeBitSet(out, (1L << LIGHT_SECTION_COUNT) - 1);
        VanillaProtocol26_2.writeVarInt(out, 0);
        VanillaProtocol26_2.writeVarInt(out, 0);
    }

    private void writeBitSet(DataOutputStream out, long bits) throws IOException {
        VanillaProtocol26_2.writeVarInt(out, 1);
        out.writeLong(bits);
    }
}
