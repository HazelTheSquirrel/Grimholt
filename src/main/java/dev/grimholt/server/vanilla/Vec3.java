package dev.grimholt.server.vanilla;

public record Vec3(double x, double y, double z) {
    public static final Vec3 ZERO = new Vec3(0, 0, 0);
    public Vec3 add(Vec3 other) { return new Vec3(x + other.x, y + other.y, z + other.z); }
    public Vec3 multiply(double value) { return new Vec3(x * value, y * value, z * value); }
    public double lengthSquared() { return x*x + y*y + z*z; }
    public double length() { return Math.sqrt(lengthSquared()); }
    public Vec3 normalize() { double l=length(); return l==0 ? ZERO : multiply(1.0/l); }
}
