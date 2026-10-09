package dev.grimholt.server.network;

import dev.grimholt.server.vanilla.VanillaGeneratedData;
import dev.grimholt.server.vanilla.VanillaJson;
import dev.grimholt.server.vanilla.VanillaProtocol;
import dev.grimholt.server.vanilla.VanillaProtocolCodec;
import dev.grimholt.server.vanilla.VanillaSnapshot;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class VanillaConfigurationSyncTest {
    @Test
    void registryPacketsPreserveMojangProtocolIdsAndAreComplete() throws Exception {
        VanillaGeneratedData generated = new VanillaGeneratedData();
        VanillaConfigurationSync sync = new VanillaConfigurationSync(generated);
        Map<String, Object> report = VanillaJson.object(
                VanillaJson.parse(generated.require("reports/registries.json")));
        List<byte[]> packets;
        try {
            packets = sync.registryDataPackets();
        } catch (IllegalStateException failure) {
            System.err.println("REGISTRY_SYNC_DIAGNOSTIC: " + failure.getMessage());
            System.err.println("REGISTRY_REPORT_KEYS: " + report.keySet());
            throw failure;
        }

        assertTrue(packets.size() >= 20, "Expected the Snapshot 3 dynamic registry set");
        for (byte[] bytes : packets) {
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            String registryId = VanillaProtocolCodec.readIdentifier(in);
            int count = VanillaProtocol.readVarInt(in);
            Map<?, ?> registry = (Map<?, ?>) report.get(registryId);
            Map<?, ?> entries = (Map<?, ?>) registry.get("entries");
            List<Map.Entry<String, Integer>> expected = new ArrayList<>();
            for (var entry : entries.entrySet()) {
                if (entry.getKey() instanceof String key && entry.getValue() instanceof Map<?, ?> metadata
                        && metadata.get("protocol_id") instanceof Number id) {
                    expected.add(Map.entry(key, id.intValue()));
                }
            }
            expected.sort(Comparator.comparingInt(Map.Entry::getValue));
            assertEquals(expected.size(), count, registryId + " entry count");

            for (Map.Entry<String, Integer> entry : expected) {
                assertEquals(entry.getKey(), VanillaProtocolCodec.readIdentifier(in),
                        registryId + " registry entries must follow Mojang protocol_id order");
                assertFalse(VanillaProtocolCodec.readBoolean(in),
                        "The negotiated core pack owns vanilla registry NBT values");
            }
            assertEquals(0, in.available(), registryId + " must not have trailing bytes");
        }
    }

    @Test
    void onlyAcceptsTheExactCorePackVersionWhenOmittingRegistryNbt() throws Exception {
        VanillaConfigurationSync sync = new VanillaConfigurationSync(new VanillaGeneratedData());
        sync.requireCompatibleKnownPacks(Set.of("minecraft:core@" + VanillaSnapshot.VERSION));
        assertThrows(IOException.class,
                () -> sync.requireCompatibleKnownPacks(Set.of("minecraft:core@26.2")));
        assertThrows(IOException.class, () -> sync.requireCompatibleKnownPacks(Set.of()));
    }

    @Test
    void updateTagsAreBuiltFromSnapshotTagFiles() {
        VanillaConfigurationSync sync = new VanillaConfigurationSync(new VanillaGeneratedData());
        assertFalse(sync.updateTagsPackets().isEmpty(),
                "Snapshot 3 tag files must be recognized and synchronized");
    }
}
