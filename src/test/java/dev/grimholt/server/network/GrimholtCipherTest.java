package dev.grimholt.server.network;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.security.SecureRandom;
import static org.junit.jupiter.api.Assertions.*;

class GrimholtCipherTest {
    @Test void aesCfb8RoundTripsArbitraryBytes() throws Exception {
        byte[] secret = new byte[16];
        new SecureRandom().nextBytes(secret);
        byte[] plain = new byte[8192];
        new SecureRandom().nextBytes(plain);
        ByteArrayOutputStream encrypted = new ByteArrayOutputStream();
        try (OutputStream cipher = GrimholtCipher.output(encrypted, secret)) {
            cipher.write(plain);
        }
        ByteArrayOutputStream decoded = new ByteArrayOutputStream();
        try (InputStream cipher = GrimholtCipher.input(new ByteArrayInputStream(encrypted.toByteArray()), secret)) {
            cipher.transferTo(decoded);
        }
        assertArrayEquals(plain, decoded.toByteArray());
    }
}
