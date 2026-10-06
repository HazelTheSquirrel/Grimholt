package dev.grimholt.server.vanilla;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VanillaGameplayKernelTest {
    @Test void physicsAndBorderAreDeterministic() {
        assertEquals(0.0, VanillaPhysics.fallDamage(3.0));
        assertEquals(2.0, VanillaPhysics.fallDamage(5.9));
        VanillaWorldBorder border=new VanillaWorldBorder(); border.center(10,20); border.size(10);
        assertTrue(border.contains(10,20)); assertFalse(border.contains(0,0));
    }
    @Test void fluidsSpreadOnlyIntoEmptyCells() {
        VanillaChunk c=new VanillaChunk(0,0); c.load(); BlockPos p=new BlockPos(0,64,0);
        c.setFluid(p,new VanillaFluidState("minecraft:water",0,false));
        new VanillaFluidEngine().tick(c,p);
        assertFalse(c.fluid(new BlockPos(1,64,0)).isEmpty());
        c.setBlock(new BlockPos(-1,64,0),BlockState.of("minecraft:stone"));
        assertTrue(c.fluid(new BlockPos(-1,64,0)).isEmpty());
    }
    @Test void gameRuntimeAdvancesWorldSystems() {
        VanillaGameRuntime g=new VanillaGameRuntime(UUID.randomUUID());
        g.weather().setRain(true,2); g.tick(); g.tick();
        assertEquals(2,g.tickCount());
    }
    @Test void commandsRecipesAndScoreboardWork() {
        VanillaCommandDispatcher<String> d=new VanillaCommandDispatcher<>(); var seen=new StringBuilder();
        d.register("say",(s,a)->seen.append(s).append(':').append(String.join(",",a)));
        assertTrue(d.execute("bob","/say hello world")); assertEquals("bob:hello,world",seen.toString());
        VanillaScoreboard sb=new VanillaScoreboard(); sb.add("kills","bob",3); sb.add("kills","bob",2); assertEquals(5,sb.get("kills","bob"));
    }
}
