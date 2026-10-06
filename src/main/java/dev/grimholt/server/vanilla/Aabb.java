package dev.grimholt.server.vanilla;

public record Aabb(double minX,double minY,double minZ,double maxX,double maxY,double maxZ) {
    public Aabb { if (minX>maxX||minY>maxY||minZ>maxZ) throw new IllegalArgumentException("invalid AABB"); }
    public boolean intersects(Aabb o) { return minX<o.maxX&&maxX>o.minX&&minY<o.maxY&&maxY>o.minY&&minZ<o.maxZ&&maxZ>o.minZ; }
    public Aabb move(double x,double y,double z){return new Aabb(minX+x,minY+y,minZ+z,maxX+x,maxY+y,maxZ+z);}
    public Aabb expand(double x,double y,double z){return new Aabb(Math.min(minX,minX+x),Math.min(minY,minY+y),Math.min(minZ,minZ+z),Math.max(maxX,maxX+x),Math.max(maxY,maxY+y),Math.max(maxZ,maxZ+z));}
}
