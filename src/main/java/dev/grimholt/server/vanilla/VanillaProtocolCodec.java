package dev.grimholt.server.vanilla;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.util.Map;
import java.util.UUID;

/** Wire primitives owned by Grimholt for Minecraft 26.4-snapshot-3. */
public final class VanillaProtocolCodec {
    private VanillaProtocolCodec() {}

    public static String readString(InputStream in, int maxBytes) throws IOException {
        int length = VanillaProtocol.readVarInt(in);
        if (length < 0 || length > maxBytes) throw new IOException("Invalid string length: " + length);
        byte[] bytes = in.readNBytes(length);
        if (bytes.length != length) throw new EOFException("Truncated string");
        final String value;
        try {
            value = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException malformed) {
            throw new IOException("Malformed UTF-8 string", malformed);
        }
        if (value.length() > maxBytes) throw new IOException("String exceeds character limit");
        return value;
    }

    public static void writeString(OutputStream out, String value, int maxBytes) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maxBytes) throw new IOException("String exceeds byte limit");
        VanillaProtocol.writeVarInt(out, bytes.length);
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

    public static boolean readBoolean(InputStream in) throws IOException {
        int value = in.read();
        if (value < 0) throw new EOFException("Truncated boolean");
        if (value != 0 && value != 1) throw new IOException("Invalid boolean value: " + value);
        return value != 0;
    }

    public static void writeBoolean(OutputStream out, boolean value) throws IOException {
        out.write(value ? 1 : 0);
    }

    public static long readVarLong(InputStream in) throws IOException {
        long result = 0;
        int shift = 0;
        for (int i = 0; i < 10; i++) {
            int b = in.read();
            if (b < 0) throw new EOFException("Unexpected EOF in VarLong");
            if (i == 9 && (b & 0xfe) != 0) throw new IOException("VarLong overflows 64 bits");
            result |= (long) (b & 0x7f) << shift;
            if ((b & 0x80) == 0) return result;
            shift += 7;
        }
        throw new IOException("VarLong exceeds 10 bytes");
    }

    public static void writeVarLong(OutputStream out, long value) throws IOException {
        long v = value;
        do {
            int next = (int) (v & 0x7f);
            v >>>= 7;
            if (v != 0) next |= 0x80;
            out.write(next);
        } while (v != 0);
    }

    public static void writeIdentifier(OutputStream out, String value) throws IOException {
        writeString(out, value, 32767);
    }

    public static String readIdentifier(InputStream in) throws IOException {
        return readString(in, 32767);
    }

    public static void writeAngle(OutputStream out, float degrees) throws IOException {
        int packed = Math.round(degrees * 256.0f / 360.0f) & 0xff;
        out.write(packed);
    }

    public static long packBlockPosition(int x, int y, int z) {
        return ((long) (x & 0x3ffffff) << 38)
                | ((long) (z & 0x3ffffff) << 12)
                | (y & 0xfffL);
    }

    /** Writes the network-NBT form: unnamed compound root, unlike disk NBT. */
    public static byte[] writeNetworkNbtCompoundBytes(VanillaNbt.Tag compound) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writeNetworkNbtCompound(out, compound);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void writeNetworkNbtCompound(OutputStream out, VanillaNbt.Tag compound) throws IOException {
        if (compound == null) { out.write(VanillaNbt.END); return; }
        if (compound.type() != VanillaNbt.COMPOUND) throw new IllegalArgumentException("Expected compound NBT");
        out.write(VanillaNbt.COMPOUND);
        writeNetworkNbtPayload(out, compound);
    }

    @SuppressWarnings("unchecked")
    private static void writeNetworkNbtPayload(OutputStream out, VanillaNbt.Tag tag) throws IOException {
        Object value = tag.value();
        switch (tag.type()) {
            case VanillaNbt.BYTE -> out.write((Byte) value);
            case VanillaNbt.SHORT -> new DataOutputStream(out).writeShort((Short) value);
            case VanillaNbt.INT -> new DataOutputStream(out).writeInt((Integer) value);
            case VanillaNbt.LONG -> new DataOutputStream(out).writeLong((Long) value);
            case VanillaNbt.FLOAT -> new DataOutputStream(out).writeFloat((Float) value);
            case VanillaNbt.DOUBLE -> new DataOutputStream(out).writeDouble((Double) value);
            case VanillaNbt.BYTE_ARRAY -> {
                byte[] a=(byte[])value; new DataOutputStream(out).writeInt(a.length); out.write(a);
            }
            case VanillaNbt.STRING -> writeNbtUtf(out, (String) value);
            case VanillaNbt.LIST -> {
                VanillaNbt.ListValue list=(VanillaNbt.ListValue)value;
                out.write(list.elementType()); new DataOutputStream(out).writeInt(list.values().size());
                for (VanillaNbt.Tag child:list.values()) writeNetworkNbtPayload(out, child);
            }
            case VanillaNbt.COMPOUND -> {
                for (var entry:((Map<String,VanillaNbt.Tag>)value).entrySet()) {
                    VanillaNbt.Tag child=entry.getValue(); out.write(child.type()); writeNbtUtf(out, entry.getKey());
                    writeNetworkNbtPayload(out, child);
                }
                out.write(VanillaNbt.END);
            }
            case VanillaNbt.INT_ARRAY -> {
                int[] a=(int[])value; DataOutputStream d=new DataOutputStream(out); d.writeInt(a.length); for(int v:a)d.writeInt(v);
            }
            case VanillaNbt.LONG_ARRAY -> {
                long[] a=(long[])value; DataOutputStream d=new DataOutputStream(out); d.writeInt(a.length); for(long v:a)d.writeLong(v);
            }
            default -> throw new IOException("Unsupported NBT type: "+tag.type());
        }
    }

    private static void writeNbtUtf(OutputStream out, String value) throws IOException {
        byte[] bytes=value.getBytes(StandardCharsets.UTF_8);
        if(bytes.length>65535) throw new IOException("NBT string too large");
        DataOutputStream d=new DataOutputStream(out); d.writeShort(bytes.length); d.write(bytes);
    }

    public static byte[] readRemaining(InputStream in, int maxBytes) throws IOException {
        byte[] bytes=in.readNBytes(maxBytes+1);
        if(bytes.length>maxBytes) throw new IOException("Payload exceeds limit");
        return bytes;
    }

    public static byte[] readByteArray(InputStream in, int maxBytes) throws IOException {
        int length = VanillaProtocol.readVarInt(in);
        if (length < 0 || length > maxBytes) throw new IOException("Invalid byte array length: " + length);
        byte[] data = in.readNBytes(length);
        if (data.length != length) throw new EOFException("Truncated byte array");
        return data;
    }

    public static void writeByteArray(OutputStream out, byte[] data, int maxBytes) throws IOException {
        if (data.length > maxBytes) throw new IOException("Byte array exceeds limit");
        VanillaProtocol.writeVarInt(out, data.length);
        out.write(data);
    }
}
