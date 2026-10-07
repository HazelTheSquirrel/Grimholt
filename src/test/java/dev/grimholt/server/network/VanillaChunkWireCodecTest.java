package dev.grimholt.server.network;

import dev.grimholt.server.vanilla.*;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class VanillaChunkWireCodecTest {
    @Test void empty26_2ChunkSerializesDeterministically() {
        VanillaGeneratedData data = new VanillaGeneratedData();
        VanillaWorldModel world = new VanillaWorldModel(
                UUID.nameUUIDFromBytes("minecraft:overworld".getBytes()), data);
        VanillaChunk chunk = new VanillaChunk(world, 0, 0);
        chunk.load();
        byte[] first = new VanillaChunkWireCodec().encodeLevelChunkWithLight(chunk);
        byte[] second = new VanillaChunkWireCodec().encodeLevelChunkWithLight(chunk);
        assertArrayEquals(first, second);
        assertTrue(first.length > 100);
    }
}
