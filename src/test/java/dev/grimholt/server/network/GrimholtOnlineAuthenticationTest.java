package dev.grimholt.server.network;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.security.SecureRandom;
import static org.junit.jupiter.api.Assertions.*;

class GrimholtOnlineAuthenticationTest {
    @Test void digestUsesSignedMinecraftHashRepresentation() {
        GrimholtOnlineAuthentication auth = new GrimholtOnlineAuthentication();
        byte[] secret = new byte[16];
        new SecureRandom().nextBytes(secret);
        String digest = auth.serverIdDigest(secret);
        assertFalse(digest.isBlank());
        assertTrue(digest.matches("-?[0-9a-f]+"));
    }

    @Test void verifyTokenIsStableForAuthenticationInstance() {
        GrimholtOnlineAuthentication auth = new GrimholtOnlineAuthentication();
        assertArrayEquals(auth.verifyToken(), auth.verifyToken());
        assertEquals(4, auth.verifyToken().length);
    }

    @Test void sessionJsonParserAcceptsWhitespaceAndEscapes() {
        String json = "{ \"id\" : \"0123456789abcdef0123456789abcdef\", \"name\" : \"Player\\u005fname\" }";
        assertEquals("Player_name", GrimholtOnlineAuthentication.jsonString(json, "name"));
        assertEquals("0123456789abcdef0123456789abcdef",
                GrimholtOnlineAuthentication.jsonString(json, "id"));
    }

    @Test void sessionJsonParserRejectsMissingOrNonStringFields() {
        assertNull(GrimholtOnlineAuthentication.jsonString("{\"id\":42}", "id"));
        assertNull(GrimholtOnlineAuthentication.jsonString("{\"name\":null}", "name"));
    }
}
