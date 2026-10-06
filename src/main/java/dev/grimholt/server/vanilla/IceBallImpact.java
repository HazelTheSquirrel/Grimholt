package dev.grimholt.server.vanilla;

public record IceBallImpact(double damage, double knockbackX, double knockbackY, double knockbackZ) {
    public static final double BASE_DAMAGE = 4.0;

    public static IceBallImpact fromVelocity(double vx, double vy, double vz) {
        double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
        double damage = Math.min(BASE_DAMAGE, BASE_DAMAGE * Math.max(0.0, speed));
        double horizontal = Math.sqrt(vx * vx + vz * vz);
        double scale = horizontal == 0.0 ? 0.0 : 0.4 / horizontal;
        return new IceBallImpact(damage, vx * scale, 0.15, vz * scale);
    }
}
