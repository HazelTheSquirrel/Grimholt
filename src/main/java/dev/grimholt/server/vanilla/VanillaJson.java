package dev.grimholt.server.vanilla;

import java.util.*;

/** Small dependency-free JSON parser used only for Mojang-generated reports. */
public final class VanillaJson {
    private final String input;
    private int p;

    private VanillaJson(String input) { this.input = Objects.requireNonNull(input); }

    public static Object parse(String input) {
        VanillaJson parser = new VanillaJson(input);
        Object value = parser.value();
        parser.ws();
        if (parser.p != parser.input.length()) throw parser.error("Trailing JSON");
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String,Object> object(Object value) {
        if (!(value instanceof Map<?,?> map)) throw new IllegalArgumentException("Expected JSON object");
        return (Map<String,Object>) map;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> array(Object value) {
        if (!(value instanceof List<?> list)) throw new IllegalArgumentException("Expected JSON array");
        return (List<Object>) list;
    }

    private Object value() {
        ws();
        if (p >= input.length()) throw error("Unexpected end");
        return switch (input.charAt(p)) {
            case '{' -> objectValue();
            case '[' -> arrayValue();
            case '"' -> string();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> number();
        };
    }

    private Map<String,Object> objectValue() {
        Map<String,Object> result = new LinkedHashMap<>();
        p++; ws();
        if (take('}')) return result;
        while (true) {
            ws();
            if (p >= input.length() || input.charAt(p) != '"') throw error("Object key expected");
            String key = string();
            ws(); expect(':');
            result.put(key, value());
            ws();
            if (take('}')) return result;
            expect(',');
        }
    }

    private List<Object> arrayValue() {
        List<Object> result = new ArrayList<>();
        p++; ws();
        if (take(']')) return result;
        while (true) {
            result.add(value());
            ws();
            if (take(']')) return result;
            expect(',');
        }
    }

    private String string() {
        expect('"');
        StringBuilder out = new StringBuilder();
        while (p < input.length()) {
            char c = input.charAt(p++);
            if (c == '"') return out.toString();
            if (c != '\') { out.append(c); continue; }
            if (p >= input.length()) throw error("Unterminated escape");
            char e = input.charAt(p++);
            switch (e) {
                case '"','\\','/' -> out.append(e);
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    if (p + 4 > input.length()) throw error("Invalid unicode escape");
                    out.append((char) Integer.parseInt(input.substring(p, p + 4), 16));
                    p += 4;
                }
                default -> throw error("Invalid escape: " + e);
            }
        }
        throw error("Unterminated string");
    }

    private Object number() {
        int start = p;
        if (input.charAt(p) == '-') p++;
        while (p < input.length() && Character.isDigit(input.charAt(p))) p++;
        boolean decimal = false;
        if (p < input.length() && input.charAt(p) == '.') {
            decimal = true; p++;
            while (p < input.length() && Character.isDigit(input.charAt(p))) p++;
        }
        if (p < input.length() && (input.charAt(p) == 'e' || input.charAt(p) == 'E')) {
            decimal = true; p++;
            if (p < input.length() && (input.charAt(p) == '+' || input.charAt(p) == '-')) p++;
            while (p < input.length() && Character.isDigit(input.charAt(p))) p++;
        }
        String n = input.substring(start, p);
        try { return decimal ? Double.parseDouble(n) : Long.parseLong(n); }
        catch (NumberFormatException e) { throw error("Invalid number: " + n); }
    }

    private Object literal(String text, Object value) {
        if (!input.startsWith(text, p)) throw error("Expected " + text);
        p += text.length();
        return value;
    }

    private void ws() { while (p < input.length() && Character.isWhitespace(input.charAt(p))) p++; }
    private void expect(char c) { if (p >= input.length() || input.charAt(p++) != c) throw error("Expected " + c); }
    private boolean take(char c) { if (p < input.length() && input.charAt(p) == c) { p++; return true; } return false; }
    private IllegalArgumentException error(String message) { return new IllegalArgumentException(message + " at offset " + p); }
}
