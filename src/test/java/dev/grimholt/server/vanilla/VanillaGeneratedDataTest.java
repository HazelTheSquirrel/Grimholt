package dev.grimholt.server.vanilla;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class VanillaGeneratedDataTest {
    @Test
    void exactSnapshotReportsArePresentAndAuthoritative() {
        VanillaGeneratedData data = new VanillaGeneratedData();
        assertTrue(data.available(), "26.4-S3 generated data must be part of the test/runtime artifact");

        VanillaBlockRegistry blocks = new VanillaBlockRegistry();
        int states = VanillaGeneratedRegistryLoader.loadBlocks(data, blocks);
        assertTrue(states > 1000, "Expected the full 26.4 block-state report");

        VanillaItemRegistry items = new VanillaItemRegistry();
        VanillaEntityRegistry entities = new VanillaEntityRegistry();
        VanillaRegistryIds itemIds = new VanillaRegistryIds();
        VanillaRegistryIds entityIds = new VanillaRegistryIds();
        int names = VanillaGeneratedRegistryLoader.loadEntityAndItemNames(data, items, entities, itemIds, entityIds);
        assertTrue(names > 1000, "Expected the full item/entity registry reports");

        assertTrue(blocks.contains("minecraft:ice"));
        assertTrue(blocks.contains("minecraft:packed_ice"));
        assertTrue(items.contains("minecraft:ice_ball"));
        assertTrue(items.contains("minecraft:ice_crystal"));
        assertTrue(entities.contains("minecraft:frostbite"));

        VanillaPacketCatalog packets = VanillaPacketCatalog.load(data);
        assertTrue(packets.size() > 100, "Expected the complete 26.4-S3 packet catalog");
        assertTrue(packets.id(VanillaProtocol26_4S3.State.LOGIN,
                VanillaProtocol26_4S3.Direction.SERVERBOUND, "minecraft:hello").isPresent());
        assertTrue(packets.id(VanillaProtocol26_4S3.State.PLAY,
                VanillaProtocol26_4S3.Direction.CLIENTBOUND, "minecraft:login").isPresent());
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
