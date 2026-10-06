package dev.grimholt.server.vanilla;

import java.util.UUID;

public record VanillaPortal(UUID destinationWorld,double x,double y,double z) {
    public VanillaPortal { if(destinationWorld==null)throw new NullPointerException("destinationWorld"); }
}
