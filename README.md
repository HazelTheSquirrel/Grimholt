# Grimholt Minestom

Grimholt is being rebuilt on Minestom with Java 25. The repository starts from a deliberately small, testable foundation rather than carrying forward the previous server implementation.

## Requirements

- JDK 25
- Maven 3.9+
- Minecraft Java Edition 26.3 client for the current compatibility branch

## Build and run

```bash
mvn -U clean verify
java -jar target/grimholt-minestom-0.1.0-SNAPSHOT.jar
```

The distributable JAR includes runtime dependencies. The default listener is `0.0.0.0:25565`.

Configure the listener with system properties or environment variables:

| Setting | System property | Environment variable | Default |
|---|---|---|---|
| Bind address | `grimholt.host` | `GRIMHOLT_HOST` | `0.0.0.0` |
| Port | `grimholt.port` | `GRIMHOLT_PORT` | `25565` |

Example:

```bash
java -Dgrimholt.host=127.0.0.1 -Dgrimholt.port=25566 -jar target/grimholt-minestom-0.1.0-SNAPSHOT.jar
```

## Minecraft protocol target

This branch uses Minestom's published `26_3-SNAPSHOT` branch from Sonatype's Maven snapshot repository. It is a moving development dependency and is intended to advance the working baseline from 26.2 to 26.3.

**Minecraft 26.4 snapshots are not claimed to be supported by this branch.** 26.4 has a different protocol version and needs a dedicated Minestom protocol/data update. We keep that as the next compatibility patch rather than changing a protocol number and risking a broken login.

The current world is intentionally a small flat bootstrap world. Persistent world storage, gameplay systems, observability, load tests, and production hardening are subsequent implementation stages; the bootstrap is not a claim that those systems are already complete.

## Architecture principles

- **Minestom owns game networking, ticking, instances, and entity scheduling.** Do not create a competing main tick loop.
- **CPU and heap detection is advisory capacity planning** for Grimholt-owned background work; it does not override Minestom's internal pools.
- **Background work is bounded.** Queue saturation rejects work explicitly instead of running expensive work on a caller thread or silently growing memory usage.
- **Hot-path optimization is evidence-driven.** Profile allocations and tick cost before introducing pooling or custom data layouts.
- **Lifecycle is explicit.** Initialize registries and the world before binding the listening socket; shut Minestom down cleanly.
- **No Bukkit, Spigot, Paper, Purpur, or NMS dependencies.**

## CI and downloadable JAR

Every push and pull request runs `mvn -U clean verify` on Java 25. The `-U` option forces Maven to check for newer snapshot artifacts instead of relying on a cached Minestom snapshot. If verification and packaging succeed, CI uploads the runnable, dependency-inclusive JAR as a GitHub Actions artifact named `grimholt-minestom-<commit-sha>` (retained for 30 days).

To download it:

1. Open the [Actions runs](https://github.com/HazelTheSquirrel/Grimholt/actions).
2. Select the successful **CI** run for the commit you want.
3. In the run's **Artifacts** section, download `grimholt-minestom-<commit-sha>`.
4. Extract the ZIP; it contains the runnable `.jar`.

The artifact is uploaded only after `mvn clean verify` succeeds. A failed or cancelled build will not publish a JAR artifact.
