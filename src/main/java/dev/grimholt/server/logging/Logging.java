package dev.grimholt.server.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class Logging {
    private static final Logger LOGGER = Logger.getLogger("Grimholt");
    private Logging() { }
    public static void startup(String address, int port) { LOGGER.info(() -> "Starting Grimholt on " + address + ":" + port); }
    public static void started() { LOGGER.info("Grimholt is running"); }
    public static void stopping() { LOGGER.info("Stopping Grimholt"); }
    public static void stopped() { LOGGER.info("Grimholt stopped"); }
    public static void shutdownStage(String stage) { LOGGER.info(() -> "Shutdown stage complete: " + stage); }
    public static void failure(Throwable throwable) { LOGGER.log(Level.SEVERE, "Grimholt startup/shutdown failure", throwable); }
}
