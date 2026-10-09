package dev.grimholt;

import dev.grimholt.runtime.RuntimeProfile;

/**
 * Diagnostic entry point for the independent Grimholt core.
 * This intentionally does not bind a Minecraft listener until transport and protocol
 * state machines are implemented and tested.
 */
public final class GrimholtServer {
    private GrimholtServer() { }

    public static void main(String[] args) {
        RuntimeProfile profile = RuntimeProfile.detect();
        System.out.println("Grimholt independent core");
        System.out.println("Java: " + Runtime.version());
        System.out.println("Available processors: " + profile.availableProcessors());
        System.out.println("Maximum heap: " + profile.maxHeapBytes() + " bytes");
        System.out.println("Recommended background workers: " + profile.backgroundWorkers());
        System.out.println("Status: foundation only; game transport and gameplay are not enabled.");
    }
}
