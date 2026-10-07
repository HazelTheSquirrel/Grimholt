package dev.grimholt.server.vanilla;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VanillaSnapshot26_4ReferenceTest {
    @Test
    void generatedReferenceContainsAuthoritative26_4Reports() {
        VanillaGeneratedData data = new VanillaGeneratedData();
        assertTrue(data.available());
        assertTrue(data.require("reports/blocks.json").contains("minecraft:"));
        assertTrue(data.require("reports/items.json").contains("minecraft:"));
        assertTrue(data.require("reports/registries.json").contains("minecraft:item"));
        assertTrue(data.require("reports/packets.json").contains("minecraft:"));
    }

    @Test
    void generatedReferenceContainsProtocolAndGameplayRegistries() {
        VanillaGeneratedData data = new VanillaGeneratedData();
        String registries = data.require("reports/registries.json");
        assertTrue(registries.contains("minecraft:entity_type"));
        assertTrue(registries.contains("minecraft:data_component_type"));
        assertTrue(registries.contains("minecraft:dimension_type"));
        assertTrue(registries.contains("minecraft:worldgen/biome"));
    }

    @Test
    void generatedTagIndexIsPresentAndDeterministic() {
        VanillaGeneratedData data = new VanillaGeneratedData();
        String index = data.require("reports/tag_files.json");
        assertTrue(index.startsWith("["));
        assertTrue(index.contains("tags/blocks/"));
        assertTrue(index.contains("tags/items/"));
        assertTrue(index.contains("tags/entity_types/"));
    }
}
