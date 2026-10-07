package dev.grimholt.server.vanilla;

import java.util.*;
import java.util.concurrent.*;

/** Repeatable 1,000-player region-owner benchmark; no shared mutable world state. */
public final class VanillaMultithreadingBenchmark {
    public record Result(int players,int regions,long operations,long elapsedNanos,double opsPerSecond){}

    public Result run(int players,int regions) throws Exception {
        if(players<1||regions<1)throw new IllegalArgumentException();
        List<VanillaWorldModel> worlds=new ArrayList<>(regions);
        for(int i=0;i<regions;i++)worlds.add(new VanillaWorldModel(UUID.randomUUID()));
        ExecutorService pool=Executors.newFixedThreadPool(Math.min(regions,Math.max(2,Runtime.getRuntime().availableProcessors())));
        Object[] owners=new Object[regions];for(int i=0;i<regions;i++)owners[i]=new Object();
        long start=System.nanoTime();List<Future<?>> futures=new ArrayList<>(players);
        for(int i=0;i<players;i++){final int n=i;final int region=n%regions;
            futures.add(pool.submit(()->{synchronized(owners[region]){
                VanillaWorldModel w=worlds.get(region);
                for(int tick=0;tick<20;tick++){
                    int x=(n*31+tick)&255,z=(n*17+tick)&255;
                    w.setBlock(new BlockPos(x,64,z),BlockState.of("minecraft:stone"));
                    w.getBlock(new BlockPos(x,64,z));
                }
            }}));
        }
        for(Future<?> f:futures)f.get();
        long elapsed=System.nanoTime()-start;pool.shutdown();pool.awaitTermination(30,TimeUnit.SECONDS);
        long ops=(long)players*20L*2L;
        return new Result(players,regions,ops,elapsed,ops/(elapsed/1_000_000_000d));
    }
}
