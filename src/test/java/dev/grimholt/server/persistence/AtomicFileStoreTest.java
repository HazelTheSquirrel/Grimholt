package dev.grimholt.server.persistence;
import static org.junit.jupiter.api.Assertions.*; import java.nio.file.Files; import org.junit.jupiter.api.Test;
class AtomicFileStoreTest {@Test void roundTripAndTraversalProtection()throws Exception{var dir=Files.createTempDirectory("grimholt-store");var store=new AtomicFileStore(dir);store.write("state.bin",new byte[]{1,2,3});assertArrayEquals(new byte[]{1,2,3},store.read("state.bin"));assertThrows(SecurityException.class,()->store.write("../escape",new byte[]{1}));}}
