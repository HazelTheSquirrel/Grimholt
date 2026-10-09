package dev.grimholt.server.vanilla;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Build-time proof that generated reports are valid inputs, not just emitted files. */
class VanillaGeneratedDataAuditTest {
    @Test void mandatoryReportsParseAndFeedRuntimeCatalogs() {
        VanillaGeneratedData data = new VanillaGeneratedData();
        assertTrue(data.available(), "Exact generated 26.4 data must be on the test classpath");

        Map<String, Object> blocks = VanillaJson.object(VanillaJson.parse(data.require("reports/blocks.json")));
        Optional<String> itemReport = data.read("reports/items.json");
        Map<String, Object> registries = VanillaJson.object(VanillaJson.parse(data.require("reports/registries.json")));
        Map<String, Object> packets = VanillaJson.object(VanillaJson.parse(data.require("reports/packets.json")));
        assertFalse(blocks.isEmpty(), "Block report must not be empty");
        if (itemReport.isPresent()) {
            assertFalse(VanillaJson.object(VanillaJson.parse(itemReport.get())).isEmpty(),
                    "Optional item report must not be empty when emitted");
        }
        Object itemRegistryValue = registries.get("minecraft:item");
        assertNotNull(itemRegistryValue, "Generated registries must include the item registry");
        Map<String, Object> itemRegistry = VanillaJson.object(itemRegistryValue);
        assertTrue(itemRegistry.get("entries") instanceof Map<?, ?> entries && !entries.isEmpty(),
                "Generated item registry must not be empty");
        assertFalse(registries.isEmpty(), "Registry report must not be empty");
        assertFalse(packets.isEmpty(), "Packet report must not be empty");

        // Protocol IDs are unique within each registry and packet direction/state.
        for (var registry : registries.entrySet()) {
            Map<String, Object> registryDefinition = VanillaJson.object(registry.getValue());
            Object rawEntries = registryDefinition.get("entries");
            if (!(rawEntries instanceof Map<?, ?> entries)) continue;
            Set<Integer> ids = new HashSet<>();
            for (var entry : entries.entrySet()) {
                if (!(entry.getValue() instanceof Map<?, ?> metadata)) continue;
                Object rawId = metadata.get("protocol_id");
                if (!(rawId instanceof Number number)) continue;
                int id = number.intValue();
                assertTrue(id >= 0, "Negative registry ID in " + registry.getKey());
                assertTrue(ids.add(id), "Duplicate registry ID " + id + " in " + registry.getKey());
            }
        }
        for (var state : packets.entrySet()) {
            Map<String, Object> directions = VanillaJson.object(state.getValue());
            for (var direction : directions.entrySet()) {
                Map<String, Object> definitions = VanillaJson.object(direction.getValue());
                Set<Integer> ids = new HashSet<>();
                for (var packet : definitions.entrySet()) {
                    Map<String, Object> definition = VanillaJson.object(packet.getValue());
                    Object rawId = definition.get("protocol_id");
                    if (!(rawId instanceof Number number)) continue;
                    int id = number.intValue();
                    assertTrue(id >= 0, "Negative packet ID in " + state.getKey() + "/" + direction.getKey());
                    assertTrue(ids.add(id), "Duplicate packet ID " + id + " in " + state.getKey() + "/" + direction.getKey());
                }
            }
        }

        // Detect duplicate state IDs and malformed/missing block identifiers before
        // the generated report can become a runtime palette source.
        Set<Integer> stateIds = new HashSet<>();
        for (var entry : blocks.entrySet()) {
            assertTrue(entry.getKey().matches("[a-z0-9_.-]+:[a-z0-9_./-]+"), "Invalid block identifier: " + entry.getKey());
            Map<String, Object> definition = VanillaJson.object(entry.getValue());
            Object rawStates = definition.get("states");
            assertInstanceOf(List.class, rawStates, "Block has no states: " + entry.getKey());
            List<Object> states = VanillaJson.array(rawStates);
            assertFalse(states.isEmpty(), "Block has empty states: " + entry.getKey());
            for (Object rawState : states) {
                Map<String, Object> state = VanillaJson.object(rawState);
                assertInstanceOf(Number.class, state.get("id"), "Block state has no numeric ID: " + entry.getKey());
                int id = ((Number) state.get("id")).intValue();
                assertTrue(id >= 0, "Negative block state ID: " + entry.getKey());
                assertTrue(stateIds.add(id), "Duplicate block state ID: " + id);
            }
        }

        VanillaGeneratedRegistryCatalog registryCatalog = VanillaGeneratedRegistryCatalog.load(data);
        VanillaPacketCatalog packetCatalog = VanillaPacketCatalog.load(data);
        assertTrue(registryCatalog.registryCount() > 0, "Runtime registry catalog must consume generated registries");
        assertTrue(packetCatalog.size() > 0, "Runtime packet catalog must consume generated packet IDs");

        List<Object> tagPaths = VanillaJson.array(VanillaJson.parse(data.require("reports/tag_files.json")));
        List<String> paths = tagPaths.stream().map(value -> assertInstanceOf(String.class, value)).toList();
        assertEquals(paths.stream().sorted().toList(), paths, "Tag report paths must be deterministic");
        assertEquals(paths.size(), new HashSet<>(paths).size(), "Duplicate tag report path");
        for (String path : paths) {
            assertTrue(path.startsWith("data/minecraft/tags/") && path.endsWith(".json"), "Invalid tag path: " + path);
            VanillaJson.parse(data.require(path));
        }
    }
}
