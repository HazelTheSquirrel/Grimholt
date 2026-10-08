# Dependency policy

## Hard bans

Grimholt runtime and public API must not depend on Bukkit, Spigot, Paper, Folia, Velocity or another Minecraft server implementation. Direct and transitive dependencies are in scope.

## Minestom

The external `net.minestom:minestom` runtime dependency is absent from the current Gradle dependency declaration/runtime graph audit. Grimholt code must not silently reintroduce it. Minestom may be studied as technical/reference material; any source adoption must be license-reviewed and deliberately imported, modified and owned by Grimholt rather than hidden behind a permanent runtime dependency.

The dependency audit is a useful guard but is not a complete proof by itself. Review direct/transitive dependency graphs, packaged JAR contents and source imports during release review.

## Third-party dependency checklist

For every addition:
- purpose and alternatives;
- version pin and update policy;
- license and notices;
- transitive dependencies;
- thread safety and allocation profile;
- startup/shutdown and native requirements;
- security advisories;
- public API leakage;
- effect on standalone packaging.

## Reference JAR

The pinned Mojang server JAR is only for checksum-verified data generation and isolated reference tests. It must never be on Grimholt's runtime classpath or bundled into the standalone JAR. The license/usage boundary must be respected; do not redistribute generated or proprietary contents beyond what is permitted.
