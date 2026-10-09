package dev.grimholt.server.vanilla;

import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VanillaNbtTest {
    @Test
    void roundTripsCompoundValues() {
        VanillaNbt.Tag root = VanillaNbt.compound(Map.of(
                "name", VanillaNbt.string("Grimholt"),
                "level", VanillaNbt.integer(26)
        ));
        assertEquals(root, VanillaNbt.read(VanillaNbt.write(root)));
    }

    @Test
    void roundTripsEmptyEndTypeList() {
        VanillaNbt.Tag root = VanillaNbt.compound(Map.of(
                "empty", VanillaNbt.list(VanillaNbt.END, java.util.List.of())
        ));
        assertEquals(root, VanillaNbt.read(VanillaNbt.write(root)));
    }

    @Test
    void rejectsTruncatedByteArrays() {
        byte[] truncated = {10, 0, 0, 7, 0, 0, 0, 0, 0, 2, 42};
        assertThrows(UncheckedIOException.class, () -> VanillaNbt.read(truncated));
    }

    @Test
    void rejectsNegativeCollectionLengthsBeforeAllocation() {
        byte[] negativeByteArrayLength = {10, 0, 0, 7, 0, 0, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
        byte[] negativeListLength = {10, 0, 0, 9, 0, 0, 3, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
        assertThrows(UncheckedIOException.class, () -> VanillaNbt.read(negativeByteArrayLength));
        assertThrows(UncheckedIOException.class, () -> VanillaNbt.read(negativeListLength));
    }

    @Test
    void rejectsTrailingBytesAfterRoot() {
        byte[] trailing = {10, 0, 0, 0, 99};
        assertThrows(UncheckedIOException.class, () -> VanillaNbt.read(trailing));
    }

    @Test
    void rejectsMalformedUtf8() {
        byte[] malformedName = {10, 0, 1, (byte) 0xC3, 0};
        assertThrows(UncheckedIOException.class, () -> VanillaNbt.read(malformedName));
    }

    @Test
    void rejectsUnknownTagTypes() {
        byte[] unknownRootType = {13, 0, 0};
        assertThrows(UncheckedIOException.class, () -> VanillaNbt.read(unknownRootType));
    }

    @Test
    void rejectsExcessiveNesting() {
        VanillaNbt.Tag nested = VanillaNbt.integer(1);
        for (int i = 0; i < 70; i++) {
            nested = VanillaNbt.compound(Map.of("nested", nested));
        }
        assertThrows(UncheckedIOException.class, () -> VanillaNbt.read(VanillaNbt.write(nested)));
    }
}
