package dev.grimholt.server.vanilla;

import java.util.*;

/**
 * Immutable, generated view of every registry exposed by Mojang's pinned
 * reports/registries.json. Gameplay systems can query registry membership and
 * wire IDs without maintaining hand-written partial copies of vanilla data.
 */
public final class VanillaGeneratedRegistryCatalog {
    private final Map<String, Map<String, Integer>> registries;

    private VanillaGeneratedRegistryCatalog(Map<String, Map<String, Integer>> registries) {
        Map<String, Map<String, Integer>> copy = new TreeMap<>();
        registries.forEach((id, entries) -> copy.put(id, Map.copyOf(entries)));
        this.registries = Collections.unmodifiableMap(copy);
    }

    public static VanillaGeneratedRegistryCatalog load(VanillaGeneratedData data) {
        Objects.requireNonNull(data, "data");
        Map<String, Object> root = VanillaJson.object(
                VanillaJson.parse(data.require("reports/registries.json")));
        Map<String, Map<String, Integer>> loaded = new TreeMap<>();

        for (var registry : root.entrySet()) {
            if (!(registry.getKey() instanceof String registryId) || registryId.isBlank()) continue;
            if (!(registry.getValue() instanceof Map<?, ?> registryValue)) continue;
            Object rawEntries = registryValue.get("entries");
            if (!(rawEntries instanceof Map<?, ?> entries)) continue;

            Map<String, Integer> ids = new TreeMap<>();
            for (var entry : entries.entrySet()) {
                if (!(entry.getKey() instanceof String entryId) || entryId.isBlank()) continue;
                int protocolId = -1;
                if (entry.getValue() instanceof Map<?, ?> metadata
                        && metadata.get("protocol_id") instanceof Number number) {
                    protocolId = number.intValue();
                    if (protocolId < 0) {
                        throw new IllegalStateException("Negative protocol ID in generated registry "
                                + registryId + ": " + entryId);
                    }
                }
                ids.put(entryId, protocolId);
            }
            loaded.put(registryId, ids);
        }

        if (loaded.isEmpty()) {
            throw new IllegalStateException("Mojang registry report contains no registry entries");
        }
        return new VanillaGeneratedRegistryCatalog(loaded);
    }

    public Set<String> registryIds() {
        return registries.keySet();
    }

    public int registryCount() {
        return registries.size();
    }

    public Set<String> entries(String registryId) {
        Map<String, Integer> entries = registries.get(registryId);
        return entries == null ? Set.of() : entries.keySet();
    }

    public boolean contains(String registryId, String entryId) {
        Map<String, Integer> entries = registries.get(registryId);
        return entries != null && entries.containsKey(entryId);
    }

    public OptionalInt protocolId(String registryId, String entryId) {
        Map<String, Integer> entries = registries.get(registryId);
        if (entries == null) return OptionalInt.empty();
        Integer id = entries.get(entryId);
        return id == null || id < 0 ? OptionalInt.empty() : OptionalInt.of(id);
    }
}
