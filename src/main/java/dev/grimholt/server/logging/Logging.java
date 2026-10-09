package dev.grimholt.server.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class Logging {
    private static final Logger LOGGER = Logger.getLogger("Grimholt");
    private Logging() { }
    public static void startup(String address, int port) { LOGGER.info(() -> "Starting Grimholt on " + address + ":" + port); }
    public static void vanillaKernelReady(int worldCount) {
        LOGGER.info(() -> "Vanilla kernel ready, world registered (worlds=" + worldCount + ")");
    }
    public static void started() { LOGGER.info("Grimholt is running"); }
    public static void stopping() { LOGGER.info("Stopping Grimholt"); }
    public static void stopped() { LOGGER.info("Grimholt stopped"); }
    public static void shutdownStage(String stage) { LOGGER.info(() -> "Shutdown stage complete: " + stage); }
    public static void connectionOpened(String remoteAddress) {
        LOGGER.info(() -> "Accepted Minecraft connection from " + remoteAddress);
    }
    public static void connectionClosed(String remoteAddress, String state, String reason) {
        LOGGER.info(() -> "Minecraft connection closed: remote=" + remoteAddress
                + ", state=" + state + ", reason=" + reason);
    }
    public static void connectionProtocol(String remoteAddress, String state, String detail) {
        LOGGER.info(() -> "Minecraft protocol: remote=" + remoteAddress
                + ", state=" + state + ", " + detail);
    }
    public static void connectionFailure(String remoteAddress, String state, Throwable throwable) {
        LOGGER.log(Level.WARNING, "Minecraft connection failed: remote=" + remoteAddress
                + ", state=" + state + ", reason=" + throwable.getMessage(), throwable);
    }
    public static void networkFailure(Throwable throwable) {
        LOGGER.log(Level.SEVERE, "Grimholt network accept loop failed", throwable);
    }
    public static void failure(Throwable throwable) { LOGGER.log(Level.SEVERE, "Grimholt startup/shutdown failure", throwable); }
}
