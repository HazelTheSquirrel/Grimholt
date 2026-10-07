package dev.grimholt.server.vanilla;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Exact packet ID catalog loaded from Mojang's generated reports/packets.json.
 */
public final class VanillaPacketCatalog {
    public record PacketKey(VanillaProtocol26_4S3.State state,
                             VanillaProtocol26_4S3.Direction direction,
                             String name) {}

    private final Map<PacketKey, Integer> ids;

    private VanillaPacketCatalog(Map<PacketKey, Integer> ids) {
        this.ids = Map.copyOf(ids);
    }

    public static VanillaPacketCatalog load(VanillaGeneratedData data) {
        Objects.requireNonNull(data, "data");
        Map<String,Object> root = VanillaJson.object(VanillaJson.parse(data.require("reports/packets.json")));
        Map<PacketKey,Integer> result = new LinkedHashMap<>();

        for (var stateEntry : root.entrySet()) {
            VanillaProtocol26_4S3.State state;
            try {
                state = VanillaProtocol26_4S3.State.valueOf(stateEntry.getKey().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            Map<String,Object> stateObject = VanillaJson.object(stateEntry.getValue());
            for (var directionEntry : stateObject.entrySet()) {
                VanillaProtocol26_4S3.Direction direction;
                try {
                    direction = VanillaProtocol26_4S3.Direction.valueOf(directionEntry.getKey().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
                Map<String,Object> packets = VanillaJson.object(directionEntry.getValue());
                for (var packet : packets.entrySet()) {
                    Map<String,Object> definition = VanillaJson.object(packet.getValue());
                    Object idValue = definition.get("protocol_id");
                    if (idValue instanceof Number number) {
                        result.put(new PacketKey(state, direction, packet.getKey()), number.intValue());
                    }
                }
            }
        }
        return new VanillaPacketCatalog(result);
    }

    public int size() { return ids.size(); }

    public OptionalInt id(VanillaProtocol26_4S3.State state,
                          VanillaProtocol26_4S3.Direction direction,
                          String name) {
        Integer id = ids.get(new PacketKey(state, direction, name));
        return id == null ? OptionalInt.empty() : OptionalInt.of(id);
    }

    public int requireId(VanillaProtocol26_4S3.State state,
                         VanillaProtocol26_4S3.Direction direction,
                         String name) {
        return id(state, direction, name).orElseThrow(() ->
                new IllegalArgumentException("Unknown 26.4-S3 packet: " + state + "/" + direction + "/" + name));
    }

    public Set<String> names(VanillaProtocol26_4S3.State state,
                             VanillaProtocol26_4S3.Direction direction) {
        return ids.keySet().stream()
                .filter(key -> key.state() == state && key.direction() == direction)
                .map(PacketKey::name)
                .collect(Collectors.toUnmodifiableSet());
    }
}
