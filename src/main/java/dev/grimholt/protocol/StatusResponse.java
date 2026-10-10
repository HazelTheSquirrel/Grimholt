package dev.grimholt.protocol;

import java.util.Objects;

/** Immutable values serialized into the JSON object sent for a STATUS request. */
public record StatusResponse(String description, String versionName, int protocolVersion,
                             int maxPlayers, int onlinePlayers) {
    public StatusResponse {
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(versionName, "versionName");
        if (protocolVersion < 0) throw new IllegalArgumentException("protocolVersion must be non-negative");
        if (maxPlayers < 0) throw new IllegalArgumentException("maxPlayers must be non-negative");
        if (onlinePlayers < 0 || onlinePlayers > maxPlayers) {
            throw new IllegalArgumentException("onlinePlayers must be between zero and maxPlayers");
        }
    }

    /**
     * Creates the status JSON expected by the protocol. This is request-path work,
     * not tick-loop work; StringBuilder avoids fragile hand-written JSON concatenation.
     */
    public String toJson() {
        return new StringBuilder(128 + description.length() + versionName.length())
                .append("{\"version\":{\"name\":\"")
                .append(escapeJson(versionName))
                .append("\",\"protocol\":").append(protocolVersion)
                .append("},\"players\":{\"max\":").append(maxPlayers)
                .append(",\"online\":").append(onlinePlayers)
                .append("},\"description\":{\"text\":\"")
                .append(escapeJson(description))
                .append("\"}}")
                .toString();
    }

    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append("\\u00");
                        escaped.append(Character.forDigit((c >>> 4) & 0xf, 16));
                        escaped.append(Character.forDigit(c & 0xf, 16));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
