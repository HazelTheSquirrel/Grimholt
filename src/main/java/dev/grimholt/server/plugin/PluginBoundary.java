package dev.grimholt.server.plugin;

import java.util.List;

/** Internal boundary reserved for the full Grimholt plugin platform implemented in Phase 2. */
public final class PluginBoundary {
    private boolean started;
    public void start() {
        if (started) throw new IllegalStateException("Plugin boundary already started");
        started = true;
    }
    public List<String> discover() {
        if (!started) throw new IllegalStateException("Plugin boundary is not started");
        return List.of();
    }
    public void stop() { started = false; }
}
