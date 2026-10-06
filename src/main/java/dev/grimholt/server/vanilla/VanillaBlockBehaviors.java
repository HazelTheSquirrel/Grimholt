package dev.grimholt.server.vanilla;

public final class VanillaBlockBehaviors {
    private VanillaBlockBehaviors() {}
    public static boolean canPlaceSnow(BlockState below){return below.id().equals("minecraft:packed_ice")||below.id().equals("minecraft:snow")||below.id().equals("minecraft:ice")||below.id().equals("minecraft:stone");}
    public static boolean isSolid(BlockState state){return !state.id().equals("minecraft:air")&&!state.id().equals("minecraft:water")&&!state.id().equals("minecraft:lava");}
    public static boolean isFlammable(BlockState state){return state.id().contains("wood")||state.id().contains("planks")||state.id().contains("leaves");}
}
