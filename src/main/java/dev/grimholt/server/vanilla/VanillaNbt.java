package dev.grimholt.server.vanilla;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.util.Objects.requireNonNull;

/** Vanilla-compatible big-endian NBT codec used by Grimholt persistence and differential tests. */
public final class VanillaNbt {
    public static final byte END=0, BYTE=1, SHORT=2, INT=3, LONG=4, FLOAT=5, DOUBLE=6,
            BYTE_ARRAY=7, STRING=8, LIST=9, COMPOUND=10, INT_ARRAY=11, LONG_ARRAY=12;

    private VanillaNbt() {}

    public record Tag(byte type, Object value) {}
    public record ListValue(byte elementType, List<Tag> values) {}

    public static byte[] write(Tag root) {
        requireNonNull(root, "root");
        if (root.type() != COMPOUND) throw new IllegalArgumentException("Root NBT tag must be a compound");
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(out);
            writeTag(data, "", root);
            data.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Tag read(byte[] bytes) {
        requireNonNull(bytes, "bytes");
        try {
            return readTag(new DataInputStream(new ByteArrayInputStream(bytes)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Tag compound(Map<String,Tag> values) {
        return new Tag(COMPOUND, Map.copyOf(values));
    }
    public static Tag string(String value) { return new Tag(STRING, requireNonNull(value)); }
    public static Tag integer(int value) { return new Tag(INT, value); }
    public static Tag longValue(long value) { return new Tag(LONG, value); }
    public static Tag list(byte elementType, List<Tag> values) {
        if (elementType == END) throw new IllegalArgumentException("NBT lists cannot use TAG_End");
        for (Tag value : values) if (value.type() != elementType) {
            throw new IllegalArgumentException("NBT list element type mismatch");
        }
        return new Tag(LIST, new ListValue(elementType, List.copyOf(values)));
    }

    @SuppressWarnings("unchecked")
    private static void writeTag(DataOutputStream out, String name, Tag tag) throws IOException {
        out.writeByte(tag.type());
        if (tag.type() == END) return;
        writeUtf(out, name);
        writePayload(out, tag.type(), tag.value());
    }

    @SuppressWarnings("unchecked")
    private static void writePayload(DataOutputStream out, byte type, Object value) throws IOException {
        switch (type) {
            case BYTE -> out.writeByte((Byte) value);
            case SHORT -> out.writeShort((Short) value);
            case INT -> out.writeInt((Integer) value);
            case LONG -> out.writeLong((Long) value);
            case FLOAT -> out.writeFloat((Float) value);
            case DOUBLE -> out.writeDouble((Double) value);
            case BYTE_ARRAY -> {
                byte[] a = (byte[]) value;
                out.writeInt(a.length);
                out.write(a);
            }
            case STRING -> writeUtf(out, (String) value);
            case LIST -> {
                ListValue list = (ListValue) value;
                out.writeByte(list.elementType());
                out.writeInt(list.values().size());
                for (Tag tag : list.values()) writePayload(out, list.elementType(), tag.value());
            }
            case COMPOUND -> {
                for (var entry : ((Map<String,Tag>) value).entrySet()) writeTag(out, entry.getKey(), entry.getValue());
                out.writeByte(END);
            }
            case INT_ARRAY -> {
                int[] a = (int[]) value;
                out.writeInt(a.length);
                for (int v : a) out.writeInt(v);
            }
            case LONG_ARRAY -> {
                long[] a = (long[]) value;
                out.writeInt(a.length);
                for (long v : a) out.writeLong(v);
            }
            default -> throw new IOException("Unsupported NBT type " + type);
        }
    }

    private static Tag readTag(DataInputStream in) throws IOException {
        byte type = in.readByte();
        if (type == END) return new Tag(END, null);
        readUtf(in); // Root/compound entry names are structural; callers receive the value tree.
        return new Tag(type, readPayload(in, type));
    }

    private static Object readPayload(DataInputStream in, byte type) throws IOException {
        return switch (type) {
            case BYTE -> in.readByte();
            case SHORT -> in.readShort();
            case INT -> in.readInt();
            case LONG -> in.readLong();
            case FLOAT -> in.readFloat();
            case DOUBLE -> in.readDouble();
            case BYTE_ARRAY -> {
                int length = checkedLength(in.readInt());
                yield in.readNBytes(length);
            }
            case STRING -> readUtf(in);
            case LIST -> {
                byte elementType = in.readByte();
                if (elementType == END) throw new IOException("TAG_End is invalid as a list element type");
                int length = checkedLength(in.readInt());
                List<Tag> values = new ArrayList<>(length);
                for (int i = 0; i < length; i++) values.add(new Tag(elementType, readPayload(in, elementType)));
                yield new ListValue(elementType, List.copyOf(values));
            }
            case COMPOUND -> {
                Map<String,Tag> values = new LinkedHashMap<>();
                while (true) {
                    byte entryType = in.readByte();
                    if (entryType == END) break;
                    String name = readUtf(in);
                    values.put(name, new Tag(entryType, readPayload(in, entryType)));
                }
                yield Map.copyOf(values);
            }
            case INT_ARRAY -> {
                int length = checkedLength(in.readInt());
                int[] values = new int[length];
                for (int i = 0; i < length; i++) values[i] = in.readInt();
                yield values;
            }
            case LONG_ARRAY -> {
                int length = checkedLength(in.readInt());
                long[] values = new long[length];
                for (int i = 0; i < length; i++) values[i] = in.readLong();
                yield values;
            }
            default -> throw new IOException("Unsupported NBT type " + type);
        };
    }

    private static int checkedLength(int length) throws IOException {
        if (length < 0 || length > 16_777_216) throw new IOException("Invalid NBT array/list length: " + length);
        return length;
    }

    private static void writeUtf(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 65_535) throw new UTFDataFormatException("NBT UTF-8 string exceeds 65535 bytes");
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    private static String readUtf(DataInputStream in) throws IOException {
        int length = in.readUnsignedShort();
        byte[] bytes = in.readNBytes(length);
        if (bytes.length != length) throw new EOFException("Truncated NBT UTF-8 string");
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
