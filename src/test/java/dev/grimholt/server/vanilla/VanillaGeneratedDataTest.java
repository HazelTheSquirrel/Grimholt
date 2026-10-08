package dev.grimholt.server.vanilla;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class VanillaGeneratedDataTest {
    @Test
    void exactSnapshotReportsArePresentAndAuthoritative() {
        VanillaGeneratedData data = new VanillaGeneratedData();
        assertTrue(data.available(), "26.4-snapshot-3 generated data must be part of the test/runtime artifact");

        VanillaBlockRegistry blocks = new VanillaBlockRegistry();
        int states = VanillaGeneratedRegistryLoader.loadBlocks(data, blocks);
        assertTrue(states > 1000, "Expected the full 26.4-snapshot-3 block-state report");

        VanillaItemRegistry items = new VanillaItemRegistry();
        VanillaEntityRegistry entities = new VanillaEntityRegistry();
        VanillaRegistryIds itemIds = new VanillaRegistryIds();
        VanillaRegistryIds entityIds = new VanillaRegistryIds();
        int names = VanillaGeneratedRegistryLoader.loadEntityAndItemNames(data, items, entities, itemIds, entityIds);
        assertTrue(names > 1000, "Expected the full item/entity registry reports");

        assertTrue(blocks.contains("minecraft:sulfur"));
        assertTrue(blocks.contains("minecraft:cinnabar"));
        assertTrue(items.contains("minecraft:sulfur"));
        assertTrue(items.contains("minecraft:sulfur_cube_bucket"));
        assertTrue(entities.contains("minecraft:sulfur_cube"));

        VanillaPacketCatalog packets = VanillaPacketCatalog.load(data);
        assertTrue(packets.size() > 100, "Expected the complete 26.4-snapshot-3 packet catalog");
        assertTrue(packets.id(VanillaProtocol26_2.State.LOGIN,
                VanillaProtocol26_2.Direction.SERVERBOUND, "minecraft:hello").isPresent());
        assertTrue(packets.id(VanillaProtocol26_2.State.PLAY,
                VanillaProtocol26_2.Direction.CLIENTBOUND, "minecraft:login").isPresent());
    }

    @Test
    void jsonParserHandlesRealWorldEscapes() {
        Map<String,Object> object = VanillaJson.object(VanillaJson.parse(
                "{\"quote\":\"a\\\\b\\\"c\",\"unicode\":\"\\u2603\"}"
        ));
        assertEquals("a\\b\"c", object.get("quote"));
        assertEquals("☃", object.get("unicode"));
    }
    @Test
    void generatedRegistryCatalogExposesAllTargetRegistryIds() {
        VanillaGeneratedRegistryCatalog catalog =
                VanillaGeneratedRegistryCatalog.load(new VanillaGeneratedData());

        assertTrue(catalog.registryCount() > 10);
        assertTrue(catalog.contains("minecraft:item", "minecraft:sulfur"));
        assertTrue(catalog.contains("minecraft:entity_type", "minecraft:sulfur_cube"));
        assertTrue(catalog.contains("minecraft:damage_type", "minecraft:generic"));
        assertTrue(catalog.protocolId("minecraft:item", "minecraft:sulfur").isPresent());
        assertFalse(catalog.contains("minecraft:item", "grimholt:not_vanilla"));
        assertTrue(catalog.entries("minecraft:item").contains("minecraft:sulfur"));
        assertThrows(UnsupportedOperationException.class,
                () -> catalog.entries("minecraft:item").add("minecraft:mutable_test"));
    }

}
