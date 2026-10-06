package dev.grimholt.api;

/** Stable logging facade exposed to Grimholt plugins. */
public interface GrimholtLogger {
    void debug(String message);
    void info(String message);
    void warn(String message);
    void error(String message);
    void error(String message, Throwable failure);
}
