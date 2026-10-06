package dev.grimholt.server.vanilla;

public record VanillaAttribute(String id,double base,double minimum,double maximum) {
    public VanillaAttribute { if(minimum>maximum||base<minimum||base>maximum) throw new IllegalArgumentException("invalid attribute"); }
    public double clamp(double value){return Math.max(minimum,Math.min(maximum,value));}
}
