# Grimholt

Grimholt is an independent Minecraft server implementation in progress, targeting Java 25. The current runnable bootstrap still uses Minestom as a temporary behavior/reference harness; it is not the intended product core. New core packages must remain independent of Minestom, and the long-term runtime will replace the bootstrap rather than grow into a Minestom-based product.

## Requirements

- JDK 25
- Maven 3.9+
- A Minecraft client compatible with the temporary Minestom bootstrap, until the independent protocol implementation is ready

## Build and run the current reference bootstrap

```bash
mvn clean verify
java -jar target/grimholt-minestom-0.1.0-SNAPSHOT.jar
```

The current distributable JAR includes runtime dependencies. The default listener is `0.0.0.0:25565`.

Configure the temporary bootstrap listener with system properties or environment variables:

| Setting | System property | Environment variable | Default |
|---|---|---|---|
| Bind address | `grimholt.host` | `GRIMHOLT_HOST` | `0.0.0.0` |
| Port | `grimholt.port` | `GRIMHOLT_PORT` | `25565` |

Example:

```bash
java -Dgrimholt.host=127.0.0.1 -Dgrimholt.port=25566 -jar target/grimholt-minestom-0.1.0-SNAPSHOT.jar
```

## Independent core principles

- **First principles, independent implementation.** Minestom is a temporary reference for observable behavior only. Do not copy, mechanically rewrite, or ship Minestom implementation code.
- **Core isolation.** New `dev.grimholt.core` code must not import Minestom. Pure core primitives and their tests should remain deterministic and dependency-light.
- **Single-writer simulation ownership.** World state will be owned by a region/actor at a time; cross-region work is messaged explicitly rather than guarded by a global world lock.
- **Bounded concurrency.** Network, persistence and generation work must have explicit queue and overload policies. Saturation must never run expensive work on a caller/simulation thread or silently grow memory.
- **Evidence-driven performance.** Track allocation rate, queue wait, p50/p95/p99 simulation latency, throughput and memory. Pooling and custom layouts require benchmark evidence.
- **Lifecycle and observability are core features.** Admission, shutdown, persistence and worker termination need explicit contracts and measurable behavior.
- **No Bukkit, Spigot, Paper, Purpur, or NMS dependencies.**

The architecture and implementation sequence are recorded in [the independent-core foundation plan](docs/INDEPENDENT_CORE_FOUNDATION.md). The flat-world bootstrap is only a behavior reference; persistent storage, full gameplay, a native protocol stack, repeatable load tests and production hardening are not yet implemented.

## CI and downloadable JAR

Every push and pull request runs `mvn clean verify` on Java 25. If verification and packaging succeed, CI uploads the runnable, dependency-inclusive JAR as a GitHub Actions artifact named `grimholt-minestom-<commit-sha>` (retained for 30 days).

To download it:

1. Open the [Actions runs](https://github.com/HazelTheSquirrel/Grimholt/actions).
2. Select the successful **CI** run for the commit you want.
3. In the run's **Artifacts** section, download `grimholt-minestom-<commit-sha>`.
4. Extract the ZIP; it contains the runnable `.jar`.

The artifact is uploaded only after `mvn clean verify` succeeds. A failed or cancelled build will not publish a JAR artifact.
