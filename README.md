# Grimholt Minestom

Grimholt is being rebuilt on Minestom with Java 25. The repository starts from a deliberately small, testable foundation rather than carrying forward the previous server implementation.

## Requirements

- JDK 25
- Maven 3.9+
- A Minecraft client compatible with the Minestom release selected in `pom.xml`

## Build and run

```bash
mvn clean verify
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

## Architecture principles

- **Minestom owns game networking, ticking, instances, and entity scheduling.** Do not create a competing main tick loop.
- **CPU and heap detection is advisory capacity planning** for Grimholt-owned background work; it does not override Minestom's internal pools.
- **Background work is bounded.** Queue saturation rejects work explicitly instead of running expensive work on a caller thread or silently growing memory usage.
- **Hot-path optimization is evidence-driven.** Profile allocations and tick cost before introducing pooling or custom data layouts.
- **Lifecycle is explicit.** Initialize registries and the world before binding the listening socket; shut Minestom down cleanly.
- **No Bukkit, Spigot, Paper, Purpur, or NMS dependencies.**

The current world is intentionally a small flat bootstrap world. Persistent world storage, gameplay systems, observability, load tests, and production hardening are subsequent implementation stages; the bootstrap is not a claim that those systems are already complete.

## CI

Every push and pull request runs `mvn clean verify` on Java 25.
