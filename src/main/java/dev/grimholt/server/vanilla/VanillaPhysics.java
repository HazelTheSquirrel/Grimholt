package dev.grimholt.server.vanilla;

public final class VanillaPhysics {
    private VanillaPhysics() {}

    public static Vec3 applyAirMotion(Vec3 velocity, boolean onGround) {
        double y = velocity.y() - 0.08;
        double drag = onGround ? 0.6 : 0.98;
        return new Vec3(velocity.x() * drag, y * 0.98, velocity.z() * drag);
    }

    public static Vec3 applyFluidMotion(Vec3 velocity, VanillaFluidState fluid) {
        if (fluid.isEmpty()) return velocity;
        double drag = fluid.id().equals("minecraft:water") ? 0.8 : 0.5;
        double lift = fluid.id().equals("minecraft:water") ? 0.02 : 0.005;
        return new Vec3(velocity.x() * drag, velocity.y() * drag + lift, velocity.z() * drag);
    }

    public static double fallDamage(double distance) {
        return Math.max(0.0, Math.floor(distance - 3.0));
    }
}
