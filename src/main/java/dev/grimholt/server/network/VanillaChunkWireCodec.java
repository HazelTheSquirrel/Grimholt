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
            int[] states = new int[4096];
            Map<Integer, Integer> paletteIndex = new LinkedHashMap<>();
            for (int ly = 0; ly < 16; ly++) {
                for (int lz = 0; lz < 16; lz++) {
                    for (int lx = 0; lx < 16; lx++) {
                        BlockPos pos = new BlockPos(chunk.chunkX() * 16 + lx, sectionY * 16 + ly,
                                chunk.chunkZ() * 16 + lz);
                        BlockState state = chunk.block(pos);
                        int id = chunk.worldModel().blockRegistry().stateId(state);
                        paletteIndex.computeIfAbsent(id, ignored -> paletteIndex.size());
                        states[(ly << 8) | (lz << 4) | lx] = id;
                        if (id != air) nonAir++;
                    }
                }
            }
            // Chunk sections contain only the non-air count followed by the two
            // paletted containers. An extra fluid-count short corrupts every section.
            sectionOut.writeShort(nonAir);
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
        // Heightmaps use padded values-per-long packing (7 nine-bit values per long),
        // not a continuous bit stream crossing long boundaries.
        long[] surface = new long[(256 + 64 / 9 - 1) / (64 / 9)];
        long[] motion = new long[surface.length];
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
                setPadded(surface, z * 16 + x, value, 9);
                setPadded(motion, z * 16 + x, value, 9);
            }
        }
        return VanillaNbt.compound(Map.of(
                "MOTION_BLOCKING", new VanillaNbt.Tag(VanillaNbt.LONG_ARRAY, motion),
                "WORLD_SURFACE", new VanillaNbt.Tag(VanillaNbt.LONG_ARRAY, surface)));
    }

    private static void setPadded(long[] data, int index, int value, int bits) {
        int valuesPerLong = 64 / bits;
        int word = index / valuesPerLong;
        int offset = (index % valuesPerLong) * bits;
        data[word] |= ((long) value & ((1L << bits) - 1)) << offset;
    }

    private void writePalettedContainer(DataOutputStream out, Map<Integer,Integer> palette, int[] values) throws IOException {
        if (palette.size() == 1) {
            writeSinglePalette(out, palette.keySet().iterator().next());
            return;
        }

        // Local palettes are capped at eight bits for blocks. Larger palettes use
        // the global block-state registry and carry raw state IDs, with no palette list.
        int localBits = Math.max(4, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
        if (localBits <= 8) {
            out.writeByte(localBits);
            VanillaProtocol26_2.writeVarInt(out, palette.size());
            for (int globalId : palette.keySet()) VanillaProtocol26_2.writeVarInt(out, globalId);
            int valuesPerLong = 64 / localBits;
            int longCount = (values.length + valuesPerLong - 1) / valuesPerLong;
            VanillaProtocol26_2.writeVarInt(out, longCount);
            for (int base = 0; base < values.length; base += valuesPerLong) {
                long packed = 0;
                for (int i = 0; i < valuesPerLong && base + i < values.length; i++) {
                    int localId = palette.get(values[base + i]);
                    packed |= ((long) localId) << (i * localBits);
                }
                out.writeLong(packed);
            }
            return;
        }

        int maxStateId = palette.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        int globalBits = Math.max(9, 32 - Integer.numberOfLeadingZeros(maxStateId));
        out.writeByte(globalBits);
        int valuesPerLong = 64 / globalBits;
        int longCount = (values.length + valuesPerLong - 1) / valuesPerLong;
        VanillaProtocol26_2.writeVarInt(out, longCount);
        for (int base = 0; base < values.length; base += valuesPerLong) {
            long packed = 0;
            for (int i = 0; i < valuesPerLong && base + i < values.length; i++) {
                packed |= ((long) values[base + i]) << (i * globalBits);
            }
            out.writeLong(packed);
        }
    }

    private void writeSinglePalette(DataOutputStream out, int value) throws IOException {
        // A zero-bit single-value palette has no packed-long array length field.
        out.writeByte(0);
        VanillaProtocol26_2.writeVarInt(out, value);
    }

    private void writeLightData(DataOutputStream out) throws IOException {
        long allSections = (1L << LIGHT_SECTION_COUNT) - 1;
        out.writeBoolean(false); // trustEdges
        writeBitSet(out, allSections); // sky light arrays are supplied for every light section
        writeBitSet(out, 0); // no block light arrays
        writeBitSet(out, 0); // no empty sky sections
        writeBitSet(out, allSections); // all block-light sections are explicitly empty

        VanillaProtocol26_2.writeVarInt(out, LIGHT_SECTION_COUNT);
        byte[] fullSky = new byte[2048];
        Arrays.fill(fullSky, (byte) 0xff);
        for (int i = 0; i < LIGHT_SECTION_COUNT; i++) {
            VanillaProtocol26_2.writeVarInt(out, fullSky.length);
            out.write(fullSky);
        }
        VanillaProtocol26_2.writeVarInt(out, 0); // no block-light arrays
    }

    private void writeBitSet(DataOutputStream out, long bits) throws IOException {
        VanillaProtocol26_2.writeVarInt(out, 1);
        out.writeLong(bits);
    }
}
