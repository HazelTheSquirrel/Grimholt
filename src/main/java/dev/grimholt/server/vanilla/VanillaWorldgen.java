package dev.grimholt.server.vanilla;

import java.util.Random;

public final class VanillaWorldgen {
    public int surfaceY(long seed,int x,int z){Random r=new Random(seed^(long)x*341873128712L^(long)z*132897987541L);return 60+r.nextInt(12);}
    public String biome(long seed,int x,int z){double n=new Random(seed^(long)x*73428767L^(long)z*912931L).nextDouble();return n<0.18?"minecraft:ice_spikes":n<0.45?"minecraft:taiga":n<0.7?"minecraft:plains":"minecraft:forest";}
    public boolean iceCave(long seed,int x,int y,int z){if(y>48)return false;long h=seed^(long)x*341873128712L^(long)y*132897987541L^(long)z*42317861L;return (h*31+17)%97==0;}
}
