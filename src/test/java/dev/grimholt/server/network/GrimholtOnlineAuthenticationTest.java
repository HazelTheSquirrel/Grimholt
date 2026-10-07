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
}
