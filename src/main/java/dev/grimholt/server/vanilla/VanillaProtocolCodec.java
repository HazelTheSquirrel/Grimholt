package dev.grimholt.server.vanilla;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Wire primitives shared by the native 26.4 protocol implementation.
 */
public final class VanillaProtocolCodec {
    private VanillaProtocolCodec() {}

    public static String readString(InputStream in, int maxBytes) throws IOException {
        int length = VanillaProtocol26_4S3.readVarInt(in);
        if (length < 0 || length > maxBytes) throw new IOException("Invalid string length: " + length);
        byte[] bytes = in.readNBytes(length);
        if (bytes.length != length) throw new EOFException("Truncated string");
        String value = new String(bytes, StandardCharsets.UTF_8);
        if (value.length() > maxBytes) throw new IOException("String exceeds character limit");
        return value;
    }

    public static void writeString(OutputStream out, String value, int maxBytes) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maxBytes) throw new IOException("String exceeds byte limit");
        VanillaProtocol26_4S3.writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    public static UUID readUuid(InputStream in) throws IOException {
        byte[] bytes = in.readNBytes(16);
        if (bytes.length != 16) throw new EOFException("Truncated UUID");
        var input = new DataInputStream(new ByteArrayInputStream(bytes));
        return new UUID(input.readLong(), input.readLong());
    }

    public static void writeUuid(OutputStream out, UUID uuid) throws IOException {
        var data = new DataOutputStream(out);
        data.writeLong(uuid.getMostSignificantBits());
        data.writeLong(uuid.getLeastSignificantBits());
    }

    public static byte[] readByteArray(InputStream in, int maxBytes) throws IOException {
        int length = VanillaProtocol26_4S3.readVarInt(in);
        if (length < 0 || length > maxBytes) throw new IOException("Invalid byte array length: " + length);
        byte[] data = in.readNBytes(length);
        if (data.length != length) throw new EOFException("Truncated byte array");
        return data;
    }

    public static void writeByteArray(OutputStream out, byte[] data, int maxBytes) throws IOException {
        if (data.length > maxBytes) throw new IOException("Byte array exceeds limit");
        VanillaProtocol26_4S3.writeVarInt(out, data.length);
        out.write(data);
    }
}
