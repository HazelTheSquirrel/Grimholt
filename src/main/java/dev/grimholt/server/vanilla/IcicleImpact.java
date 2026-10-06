package dev.grimholt.server.vanilla;

public record IcicleImpact(double fallDamage, double landingBonusDamage) {
    public static IcicleImpact fromFallDistance(double distance) {
        if (distance <= 0) return new IcicleImpact(0.0, 0.0);
        double fall = Math.max(1.0, distance);
        return new IcicleImpact(fall, fall * 0.5);
    }
}
