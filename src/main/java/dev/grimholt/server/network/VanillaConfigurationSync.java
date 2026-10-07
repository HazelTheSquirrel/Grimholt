package dev.grimholt.server.network;

import dev.grimholt.server.vanilla.*;
import java.io.*;
import java.util.*;

/**
 * Owns the 26.2 configuration bootstrap. It deliberately consumes Grimholt's
 * generated reports rather than importing another server's packet classes.
 */
public final class VanillaConfigurationSync {
    private static final List<String> SYNCHRONIZED_REGISTRIES = List.of(
            "minecraft:worldgen/biome",
            "minecraft:chat_type",
            "minecraft:trim_pattern",
            "minecraft:trim_material",
            "minecraft:wolf_variant",
            "minecraft:wolf_sound_variant",
            "minecraft:pig_variant",
            "minecraft:pig_sound_variant",
            "minecraft:frog_variant",
            "minecraft:cat_variant",
            "minecraft:cat_sound_variant",
            "minecraft:cow_variant",
            "minecraft:cow_sound_variant",
            "minecraft:chicken_variant",
            "minecraft:chicken_sound_variant",
            "minecraft:zombie_nautilus_variant",
            "minecraft:painting_variant",
            "minecraft:dimension_type",
            "minecraft:damage_type",
            "minecraft:banner_pattern",
            "minecraft:enchantment",
            "minecraft:jukebox_song",
            "minecraft:instrument",
            "minecraft:test_environment",
            "minecraft:test_instance",
            "minecraft:dialog",
            "minecraft:world_clock",
            "minecraft:timeline",
            "minecraft:sulfur_cube_archetype"
    );

    private final VanillaGeneratedData generated;

    public VanillaConfigurationSync(VanillaGeneratedData generated) {
        this.generated = Objects.requireNonNull(generated, "generated");
    }

    /** Clientbound Select Known Packs. */
    public byte[] selectKnownPacks() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            VanillaProtocol26_2.writeVarInt(out, 1);
            VanillaProtocolCodec.writeString(out, "minecraft", 32767);
            VanillaProtocolCodec.writeString(out, "core", 32767);
            VanillaProtocolCodec.writeString(out, VanillaSnapshot26_2.VERSION, 32767);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads the serverbound Select Known Packs payload. The exact client list is
     * intentionally retained only as a compatibility observation; registry
     * ordering remains server authoritative.
     */
    public Set<String> readKnownPacks(byte[] payload) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(payload);
        int count = VanillaProtocol26_2.readVarInt(in);
        if (count < 0 || count > 64) throw new IOException("Invalid known-pack count: " + count);
        Set<String> result = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) {
            String namespace = VanillaProtocolCodec.readString(in, 32767);
            String id = VanillaProtocolCodec.readString(in, 32767);
            String version = VanillaProtocolCodec.readString(in, 32767);
            result.add(namespace + ":" + id + "@" + version);
        }
        if (in.available() != 0) throw new IOException("Trailing data in known-packs packet");
        return Set.copyOf(result);
    }

    /**
     * Builds Registry Data payloads. For vanilla's core known pack the client
     * already owns the canonical entry values, so the Grimholt wire contract
     * may send entries with the optional NBT bit cleared. This keeps the
     * bootstrap deterministic while leaving custom-data overrides for the
     * future data-pack layer.
     */
    public List<byte[]> registryDataPackets() {
        if (!generated.available()) return List.of();
        Map<String,Object> root = VanillaJson.object(
                VanillaJson.parse(generated.require("reports/registries.json")));
        List<byte[]> packets = new ArrayList<>();

        for (String registryId : SYNCHRONIZED_REGISTRIES) {
            Object raw = root.get(registryId);
            if (!(raw instanceof Map<?,?> rawMap)) continue;
            Object entriesValue = rawMap.get("entries");
            if (!(entriesValue instanceof Map<?,?> entries)) continue;

            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                VanillaProtocolCodec.writeIdentifier(out, registryId);
                VanillaProtocol26_2.writeVarInt(out, entries.size());

                List<String> names = entries.keySet().stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .filter(name -> name.startsWith("minecraft:"))
                        .sorted()
                        .toList();

                VanillaProtocol26_2.writeVarInt(out, names.size());
                for (String name : names) {
                    VanillaProtocolCodec.writeIdentifier(out, name);
                    VanillaProtocolCodec.writeBoolean(out, false);
                }
                packets.add(out.toByteArray());
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot encode registry " + registryId, e);
            }
        }
        return List.copyOf(packets);
    }
}
