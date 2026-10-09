package dev.grimholt.protocol;

/** Minecraft-style signed 32-bit VarInt primitives with no intermediate allocations. */
public final class VarInt {
    private VarInt() { }

    public static int sizeOf(int value) {
        int size = 1;
        while ((value & ~0x7f) != 0) {
            value >>>= 7;
            size++;
        }
        return size;
    }

    /** Writes the value at offset and returns the number of bytes written. */
    public static int write(int value, byte[] destination, int offset) {
        checkRange(destination.length, offset, 1);
        int cursor = offset;
        while ((value & ~0x7f) != 0) {
            checkRange(destination.length, cursor, 1);
            destination[cursor++] = (byte) ((value & 0x7f) | 0x80);
            value >>>= 7;
        }
        checkRange(destination.length, cursor, 1);
        destination[cursor++] = (byte) value;
        return cursor - offset;
    }

    /**
     * Reads a VarInt without allocating. Returns -1 when the supplied range ends before
     * the value is complete; malformed or overlong values throw IllegalArgumentException.
     */
    public static int read(byte[] source, int offset, int limit) {
        if (offset < 0 || limit < offset || limit > source.length) {
            throw new IndexOutOfBoundsException("Invalid VarInt range");
        }
        int value = 0;
        for (int index = 0; index < 5; index++) {
            int cursor = offset + index;
            if (cursor >= limit) return -1;
            int current = source[cursor] & 0xff;
            // The fifth byte has only four data bits in a 32-bit VarInt and cannot continue.
            if (index == 4 && (current & 0xf0) != 0) {
                throw new IllegalArgumentException("VarInt exceeds 32 bits");
            }
            value |= (current & 0x7f) << (index * 7);
            if ((current & 0x80) == 0) return value;
        }
        throw new IllegalArgumentException("VarInt exceeds five bytes");
    }

    private static void checkRange(int length, int offset, int count) {
        if (offset < 0 || offset > length - count) {
            throw new IndexOutOfBoundsException("Destination is too small for VarInt");
        }
    }
}
