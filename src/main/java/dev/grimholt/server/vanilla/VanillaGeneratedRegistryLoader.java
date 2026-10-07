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
                BlockState blockState = new BlockState(blockId, properties);
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
        return loadEntityAndItemNames(data, items, entities, new VanillaRegistryIds(), new VanillaRegistryIds());
    }

    public static int loadEntityAndItemNames(VanillaGeneratedData data,
                                              VanillaItemRegistry items,
                                              VanillaEntityRegistry entities,
                                              VanillaRegistryIds itemIds,
                                              VanillaRegistryIds entityIds) {
        int count = 0;

        // items.json is generated from the same Mojang server.jar and contains
        // authoritative stack limits, durability and default item components.
        Optional<String> itemReport = data.read("reports/items.json");
        if (itemReport.isPresent()) {
            Map<String,Object> itemsReport = VanillaJson.object(VanillaJson.parse(itemReport.get()));
            for (var entry : itemsReport.entrySet()) {
                if (!(entry.getKey() instanceof String id) || !id.startsWith("minecraft:")) continue;
                if (!(entry.getValue() instanceof Map<?,?> raw)) continue;
                int stackSize = number(raw.get("max_stack_size"), number(raw.get("stack_size"), 64));
                int maxDamage = number(raw.get("max_damage"), 0);
                boolean edible = raw.containsKey("components") && String.valueOf(raw.get("components")).contains("minecraft:food");
                Map<String,String> components = new LinkedHashMap<>();
                if (raw.get("components") instanceof Map<?,?> componentMap) {
                    for (var component : componentMap.entrySet()) {
                        if (component.getKey() instanceof String key) {
                            components.put(key, jsonString(component.getValue()));
                        }
                    }
                }
                items.registerAuthoritative(new VanillaItemDefinition(id, Math.max(1, Math.min(64, stackSize)),
                        Math.max(0, maxDamage), edible, components));
                count++;
            }
        }

        Map<String,Object> report = VanillaJson.object(VanillaJson.parse(data.require("reports/registries.json")));
        for (var entry : report.entrySet()) {
            String registryId = entry.getKey();
            if (!(entry.getValue() instanceof Map<?,?> raw)) continue;
            Object entries = raw.get("entries");
            if (!(entries instanceof Map<?,?> map)) continue;
            for (Object key : map.keySet()) {
                if (!(key instanceof String id) || !id.startsWith("minecraft:")) continue;
                Object entryValue = map.get(key);
                int protocolId = entryValue instanceof Map<?,?> entryMap && entryMap.get("protocol_id") instanceof Number n
                        ? n.intValue() : -1;
                if (registryId.endsWith(":item") || registryId.equals("minecraft:item")) {
                    items.register(new VanillaItemDefinition(id, 64, 0, false, Map.of()));
                    if (protocolId >= 0) itemIds.register(id, protocolId);
                    count++;
                } else if (registryId.endsWith(":entity_type") || registryId.equals("minecraft:entity_type")) {
                    entities.register(id);
                    if (protocolId >= 0) entityIds.register(id, protocolId);
                    count++;
                }
            }
        }
        return count;
    }


    private static int number(Object value, int fallback) {
        return value instanceof Number n ? n.intValue() : fallback;
    }

    private static String jsonString(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        if (value instanceof Boolean || value instanceof Number) return String.valueOf(value);
        if (value instanceof List<?> list) return list.stream().map(VanillaGeneratedRegistryLoader::jsonString).collect(java.util.stream.Collectors.joining(",", "[", "]"));
        if (value instanceof Map<?,?> map) {
            return map.entrySet().stream().filter(e -> e.getKey() instanceof String)
                    .map(e -> "\""+e.getKey()+"\":"+jsonString(e.getValue()))
                    .collect(java.util.stream.Collectors.joining(",", "{", "}"));
        }
        return String.valueOf(value);
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
