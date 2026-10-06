package dev.grimholt.server.vanilla;

import java.util.Objects;

public record VanillaEffect(String id,int amplifier,int durationTicks) {
    public VanillaEffect { Objects.requireNonNull(id,"id"); if(amplifier<0||durationTicks<0) throw new IllegalArgumentException(); }
    public VanillaEffect tick(){ return new VanillaEffect(id,amplifier,Math.max(0,durationTicks-1)); }
    public boolean active(){return durationTicks>0;}
}
