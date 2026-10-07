package dev.grimholt.server.network;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

final class GrimholtCipher {
    private GrimholtCipher() {}
    static InputStream input(InputStream input, byte[] secret) throws GeneralSecurityException {
        return new CipherInputStream(input, cipher(Cipher.DECRYPT_MODE, secret));
    }
    static OutputStream output(OutputStream output, byte[] secret) throws GeneralSecurityException {
        return new CipherOutputStream(output, cipher(Cipher.ENCRYPT_MODE, secret));
    }
    private static Cipher cipher(int mode, byte[] secret) throws GeneralSecurityException {
        if (secret.length != 16) throw new GeneralSecurityException("Minecraft AES key must be 128 bit");
        Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
        SecretKeySpec key = new SecretKeySpec(secret, "AES");
        cipher.init(mode, key, new IvParameterSpec(secret));
        return cipher;
    }
}
