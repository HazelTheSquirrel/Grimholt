# Grimholt

Grimholt is an independent Minecraft Java server implementation in active development. The pinned behavior/protocol reference is **Minecraft Java Edition 26.4 Snapshot 3**.

- Reference JAR: `reference/minecraft/26.4/server.jar`
- SHA-1: `2d89c95c030e635387448f332961074ce1adbb4b`
- Build toolchain: Java 25, Gradle 9.8.0
- Active engineering branch: `test`
- Standalone artifact: `gradle assemble` → `build/libs/*-standalone.jar`

## Honest project status

The repository has a Grimholt-owned Java kernel, public plugin API, command/network abstractions, protocol codecs, resource-budget logic, persistence utilities, generated-reference tooling and CI. The external `net.minestom:minestom` runtime dependency is absent from the Gradle runtime graph.

**Grimholt is not yet a complete or client-verified Minecraft server.** A protocol handshake test, successful compilation, or successful boot of Mojang's reference server does not prove Grimholt client interoperability, gameplay parity, world generation parity, or complete persistence. The outstanding work and release gates are tracked in [the completion work order](docs/MASTER-WORKPLAN.md) and [the forensic audit](docs/FORENSIC-PARITY-AUDIT.md).

## Build and verification

```bash
gradle --no-daemon clean test
gradle --no-daemon dependencyAudit
gradle --no-daemon vanillaReferenceSmoke26_4
gradle --no-daemon assemble
```

The CI pipeline verifies the pinned reference checksum, runs tests, boots the Mojang reference JAR as a smoke test, assembles Grimholt, and uploads the standalone JAR. These are baseline checks, not a substitute for a real Minecraft client test or vanilla differential suite.

## Project documents

- [Completion work order / master plan](docs/MASTER-WORKPLAN.md)
- [Forensic parity audit](docs/FORENSIC-PARITY-AUDIT.md)
- [Compatibility evidence matrix](docs/COMPATIBILITY.md)
- [Architecture and ownership](docs/ARCHITECTURE.md)
- [Fork migration](docs/FORK-MIGRATION.md)
- [Build instructions](docs/BUILD.md)
- [Public plugin API](docs/API.md)
- [Dependency policy](docs/DEPENDENCY-POLICY.md)
- [Security](docs/SECURITY.md)
- [Performance and benchmark gates](docs/PERFORMANCE.md)
- [Minecraft version update procedure](docs/VERSION-UPDATE.md)
- [26.4 Snapshot 3 target checklist](docs/VANILLA-26.2.md)

## Non-negotiable rules

1. Grimholt must own its runtime and public API; no external Minecraft server implementation is a runtime dependency.
2. The Mojang JAR is a pinned reference/test input only and must never be bundled as Grimholt's implementation.
3. A feature is complete only when runtime wiring and executable tests demonstrate it.
4. Every concurrency-sensitive system needs an ownership contract, adversarial tests and bounded queues/backpressure.
5. Do not claim 500–1000-player capacity until repeatable networked workload benchmarks demonstrate it.
6. Keep the `test` branch as the active implementation line; do not silently change `main`.
