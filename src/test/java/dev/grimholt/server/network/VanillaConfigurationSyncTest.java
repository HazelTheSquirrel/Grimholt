package dev.grimholt.server.network;

import dev.grimholt.server.vanilla.VanillaGeneratedData;
import dev.grimholt.server.vanilla.VanillaJson;
import dev.grimholt.server.vanilla.VanillaProtocol;
import dev.grimholt.server.vanilla.VanillaProtocolCodec;
import dev.grimholt.server.vanilla.VanillaSnapshot;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class VanillaConfigurationSyncTest {
    @Test
    void registryPacketsUsePinnedDataPackEntriesAndStableOrdering() throws Exception {
        VanillaGeneratedData generated = new VanillaGeneratedData();
        VanillaConfigurationSync sync = new VanillaConfigurationSync(generated);
        Map<String, Object> report = VanillaJson.object(
                VanillaJson.parse(generated.require("reports/registry_entries.json")));
        List<byte[]> packets = sync.registryDataPackets();

        assertTrue(packets.size() >= 20, "Expected the Snapshot 3 dynamic registry set");
        for (byte[] bytes : packets) {
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            String registryId = VanillaProtocolCodec.readIdentifier(in);
            int count = VanillaProtocol.readVarInt(in);
            List<?> rawEntries = (List<?>) report.get(registryId);
            List<String> expected = rawEntries.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .sorted()
                    .toList();
            assertEquals(expected.size(), count, registryId + " entry count");

            for (String entry : expected) {
                assertEquals(entry, VanillaProtocolCodec.readIdentifier(in),
                        registryId + " entries must match the sorted pinned data-pack index");
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
