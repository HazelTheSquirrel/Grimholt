package dev.grimholt.server.vanilla;

import java.nio.file.Path;

/** @deprecated Compatibility facade; use {@link VanillaReferenceRunner}. */
@Deprecated(forRemoval = false)
public final class VanillaReferenceRunner26_2 extends VanillaReferenceRunner {
    public VanillaReferenceRunner26_2(Path jar, Path workingDirectory) {
        super(jar, workingDirectory);
    }
}
