package dev.grimholt.server.vanilla;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class VanillaGeneratedDataTest {
    @Test
    void exactSnapshotReportsArePresentAndAuthoritative() {
        VanillaGeneratedData data = new VanillaGeneratedData();
        assertTrue(data.available(), "26.2 generated data must be part of the test/runtime artifact");

        VanillaBlockRegistry blocks = new VanillaBlockRegistry();
        int states = VanillaGeneratedRegistryLoader.loadBlocks(data, blocks);
        assertTrue(states > 1000, "Expected the full 26.2 block-state report");

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
        assertTrue(packets.size() > 100, "Expected the complete 26.2 packet catalog");
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
}
