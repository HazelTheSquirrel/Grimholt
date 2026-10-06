package dev.grimholt.server.compat;
import net.minestom.server.MinecraftServer;
public final class CompatibilityProvider {
 private CompatibilityProvider(){}
 public static CompatibilityInfo current(String implementationVersion){return new CompatibilityInfo("26.2",MinecraftServer.PROTOCOL_VERSION,MinecraftServer.DATA_VERSION,implementationVersion);}
}