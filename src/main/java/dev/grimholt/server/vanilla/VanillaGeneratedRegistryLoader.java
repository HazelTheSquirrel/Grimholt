package dev.grimholt.server.vanilla;

import java.util.*;

/**
 * Loads authoritative registries from the exact server data reports.
 *
 * <p>Block state IDs are taken from reports/blocks.json rather than recreated
 * by cartesian-product guesses. This is the canonical network/state palette.</p>
 */
public final class VanillaGeneratedRegistryLoader {
    private VanillaGeneratedRegistryLoader() {}

    public static int loadBlocks(VanillaGeneratedData data, VanillaBlockRegistry registry) {
        Objects.requireNonNull(data);
        Objects.requireNonNull(registry);
        Map<String,Object> blocks = VanillaJson.object(VanillaJson.parse(data.require("reports/blocks.json")));
        int states = 0;
        for (var entry : blocks.entrySet()) {
            String blockId = entry.getKey();
            Map<String,Object> definition = VanillaJson.object(entry.getValue());
            List<Object> stateList = VanillaJson.array(definition.getOrDefault("states", List.of()));
            for (Object stateValue : stateList) {
                Map<String,Object> state = VanillaJson.object(stateValue);
                Map<String,String> properties = stringMap(state.get("properties"));
                BlockState blockState = BlockState.of(blockId, properties);
                int id = ((Number) state.get("id")).intValue();
                registry.registerAuthoritative(blockState, id, Boolean.TRUE.equals(state.get("default")));
                states++;
            }
        }
        return states;
    }

    /**
     * Loads names and numeric IDs from the generated registry report. This is
     * intentionally generic because the registry report is itself the authority.
     */
    public static int loadEntityAndItemNames(VanillaGeneratedData data,
                                              VanillaItemRegistry items,
                                              VanillaEntityRegistry entities) {
        Map<String,Object> report = VanillaJson.object(VanillaJson.parse(data.require("reports/registries.json")));
        int count = 0;
        for (var entry : report.entrySet()) {
            String registryId = entry.getKey();
            if (!(entry.getValue() instanceof Map<?,?> raw)) continue;
            Object entries = raw.get("entries");
            if (!(entries instanceof Map<?,?> map)) continue;
            for (Object key : map.keySet()) {
                if (!(key instanceof String id) || !id.startsWith("minecraft:")) continue;
                if (registryId.endsWith(":item") || registryId.equals("minecraft:item")) {
                    items.register(new VanillaItemDefinition(id, 64, 0, false, Map.of()));
                    count++;
                } else if (registryId.endsWith(":entity_type") || registryId.equals("minecraft:entity_type")) {
                    entities.register(id);
                    count++;
                }
            }
        }
        return count;
    }

    private static Map<String,String> stringMap(Object value) {
        if (!(value instanceof Map<?,?> map)) return Map.of();
        Map<String,String> result = new LinkedHashMap<>();
        for (var e : map.entrySet()) {
            if (e.getKey() instanceof String key && e.getValue() instanceof String val) {
                result.put(key, val);
            }
        }
        return result;
    }
}
