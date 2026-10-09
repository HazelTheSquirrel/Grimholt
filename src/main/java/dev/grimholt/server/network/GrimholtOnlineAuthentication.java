package dev.grimholt.server.network;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.UUID;
import javax.crypto.Cipher;

/** Grimholt-owned 26.2 online-mode authentication primitives. */
public final class GrimholtOnlineAuthentication {
    private static final URI HAS_JOINED = URI.create("https://sessionserver.mojang.com/session/minecraft/hasJoined");
    private final KeyPair keyPair;
    private final byte[] verifyToken;
    private final HttpClient http;

    public GrimholtOnlineAuthentication() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);
            keyPair = generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to initialize Grimholt online authentication", e);
        }
        verifyToken = new byte[4];
        new SecureRandom().nextBytes(verifyToken);
        http = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
    }

    public byte[] publicKey() { return keyPair.getPublic().getEncoded().clone(); }
    public byte[] verifyToken() { return verifyToken.clone(); }

    public byte[] decryptRsa(byte[] encrypted) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
        return cipher.doFinal(encrypted);
    }

    public String serverIdDigest(byte[] sharedSecret) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(sharedSecret);
            sha1.update(keyPair.getPublic().getEncoded());
            return new BigInteger(sha1.digest()).toString(16);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public AuthenticatedProfile verifyJoined(String username, String serverId) throws IOException {
        String query = HAS_JOINED + "?username=" + java.net.URLEncoder.encode(username, StandardCharsets.UTF_8)
                + "&serverId=" + java.net.URLEncoder.encode(serverId, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(query))
                .timeout(java.time.Duration.ofSeconds(10)).GET().build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) throw new IOException("Mojang session verification failed: HTTP " + response.statusCode());
            String body = response.body();
            String name = jsonString(body, "name");
            String id = jsonString(body, "id");
            if (name == null || id == null || !name.equalsIgnoreCase(username))
                throw new IOException("Mojang session response did not verify username");
            return new AuthenticatedProfile(name, parseUuid(id));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Mojang session verification interrupted", e);
        }
    }

    public record AuthenticatedProfile(String username, UUID uuid) {}

    private static String jsonString(String json, String key) {
        // JSON whitespace is insignificant; do not assume Mojang returns compact JSON.
        int field = json.indexOf("\"" + key + "\"");
        if (field < 0) return null;
        int colon = json.indexOf(':', field + key.length() + 2);
        if (colon < 0) return null;
        int cursor = colon + 1;
        while (cursor < json.length() && Character.isWhitespace(json.charAt(cursor))) cursor++;
        if (cursor >= json.length() || json.charAt(cursor++) != '"') return null;

        StringBuilder value = new StringBuilder();
        while (cursor < json.length()) {
            char ch = json.charAt(cursor++);
            if (ch == '"') return value.toString();
            if (ch != '\\') {
                value.append(ch);
                continue;
            }
            if (cursor >= json.length()) return null;
            char escaped = json.charAt(cursor++);
            switch (escaped) {
                case '"' -> value.append('"');
                case '\\' -> value.append('\\');
                case '/' -> value.append('/');
                case 'b' -> value.append('\b');
                case 'f' -> value.append('\f');
                case 'n' -> value.append('\n');
                case 'r' -> value.append('\r');
                case 't' -> value.append('\t');
                case 'u' -> {
                    if (cursor + 4 > json.length()) return null;
                    try {
                        value.append((char) Integer.parseInt(json.substring(cursor, cursor + 4), 16));
                    } catch (NumberFormatException invalidEscape) {
                        return null;
                    }
                    cursor += 4;
                }
                default -> { return null; }
            }
        }
        return null;
    }

    private static UUID parseUuid(String id) {
        if (id.length() != 32) throw new IllegalArgumentException("Invalid Mojang UUID");
        return UUID.fromString(id.substring(0,8)+"-"+id.substring(8,12)+"-"+id.substring(12,16)+"-"+id.substring(16,20)+"-"+id.substring(20));
    }
}
