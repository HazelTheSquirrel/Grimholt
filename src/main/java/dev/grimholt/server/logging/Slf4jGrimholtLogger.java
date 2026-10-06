package dev.grimholt.server.logging;

import dev.grimholt.api.GrimholtLogger;
import org.slf4j.Logger;
import java.util.Objects;

public final class Slf4jGrimholtLogger implements GrimholtLogger {
    private final Logger delegate;
    public Slf4jGrimholtLogger(Logger delegate) { this.delegate = Objects.requireNonNull(delegate, "delegate"); }
    @Override public void debug(String message) { delegate.debug(message); }
    @Override public void info(String message) { delegate.info(message); }
    @Override public void warn(String message) { delegate.warn(message); }
    @Override public void error(String message) { delegate.error(message); }
    @Override public void error(String message, Throwable failure) { delegate.error(message, failure); }
}
