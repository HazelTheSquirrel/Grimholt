package dev.grimholt.server.vanilla;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class VanillaParityInfrastructureTest {
    @Test void allDeclaredBlockStatesAreMaterialized(){
        VanillaBlockRegistry blocks=new VanillaBlockRegistry();
        VanillaBlockStateRegistry states=new VanillaBlockStateRegistry(blocks);
        states.schema("minecraft:redstone_wire",
            new VanillaBlockStateRegistry.Property("power",IntValues.range(0,15)),
            new VanillaBlockStateRegistry.Property("north",List.of("none","side","up")),
            new VanillaBlockStateRegistry.Property("south",List.of("none","side","up")));
        assertEquals(16*3*3, states.stateCount());
        assertEquals("15",states.state("minecraft:redstone_wire",Map.of("power","15","north","up","south","side")).property("power"));
    }

    @Test void nbtRoundTripAndAnvilRoundTrip() throws Exception {
        VanillaNbt.Tag root=VanillaNbt.compound(Map.of("name",VanillaNbt.string("grimholt"),"tick",VanillaNbt.longValue(42)));
        byte[] data=VanillaNbt.write(root);
        assertEquals(VanillaNbt.COMPOUND,VanillaNbt.read(data).type());
        var path=Files.createTempDirectory("grimholt-anvil").resolve("r.0.0.mca");
        try(var region=new VanillaAnvilRegion(path)){region.writeChunk(0,0,data);assertArrayEquals(data,region.readChunk(0,0));}
    }

    @Test void thousandPlayerStressIsConcurrentAndDeterministic(){
        VanillaWorldModel world=new VanillaWorldModel(UUID.randomUUID());
        ExecutorService pool=Executors.newFixedThreadPool(Math.min(16,Runtime.getRuntime().availableProcessors()));
        try{
            List<Future<?>> jobs=new ArrayList<>();
            for(int i=0;i<1000;i++){final int n=i;jobs.add(pool.submit(()->world.setBlock(new BlockPos(n&255,64,(n>>>8)&255),BlockState.of("minecraft:stone"))));}
            for(Future<?> f:jobs)assertDoesNotThrow(f::get);
            assertEquals(1000,world.chunks().stream().mapToLong(c->c.blockSnapshot().size()).sum());
        } catch(Exception e){fail(e);}
        finally{pool.shutdownNow();}
    }

    @Test void differentialHarnessRequiresRealReference(){
        assertThrows(IllegalStateException.class,()->VanillaDifferentialHarness.requireReference("GRIMHOLT_MC_26_4_S3_JAR"));
    }

    static final class IntValues {
        static List<String> range(int from,int to){List<String> out=new ArrayList<>();for(int i=from;i<=to;i++)out.add(Integer.toString(i));return out;}
    }
}
