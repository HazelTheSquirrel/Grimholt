# Grimholt

Grimholt is an independent Minecraft Java server runtime and simulation engine under active development. It owns its networking, protocol, world model, gameplay runtime and scheduling kernel; it does not depend on another Minecraft server implementation at runtime. The pinned behavior/protocol reference is **Minecraft Java Edition 26.4 Snapshot 3**.

- Reference JAR: `reference/minecraft/26.4/server.jar`
- SHA-1: `2d89c95c030e635387448f332961074ce1adbb4b`
- Build toolchain: Java 25, Gradle 9.8.0
- Active engineering branch: `test`
- Standalone artifact: `gradle assemble` → `build/libs/*-standalone.jar`

## Engineering direction: Chronos

The core architectural bet is region-parallel simulation: each mutable region has one logical writer, independent regions run concurrently, cross-region changes use bounded handoffs, and overload is measured rather than hidden behind stale queues. Chronos is wired into the production kernel and drives registered regions automatically. Full cross-region causal semantics, parity validation, and measured high-player capacity remain open work. See [the Chronos architecture](docs/CHRONOS.md).

## Honest project status

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
- [Build instructions](docs/BUILD.md)
- [Public plugin API](docs/API.md)
- [Dependency policy](docs/DEPENDENCY-POLICY.md)
- [Security](docs/SECURITY.md)
- [Performance and benchmark gates](docs/PERFORMANCE.md)
- [Minecraft version update procedure](docs/VERSION-UPDATE.md)
- [26.4 Snapshot 3 target checklist](docs/VANILLA-26.2.md)

## Non-negotiable rules

1. Grimholt owns and builds its runtime from source in this repository; no external Minecraft server implementation is required at runtime.
2. The Mojang JAR is a pinned reference/test input only and must never be bundled as Grimholt's implementation.
3. A feature is complete only when runtime wiring and executable tests demonstrate it.
4. Every concurrency-sensitive system needs an ownership contract, adversarial tests and bounded queues/backpressure.
5. Do not claim 500–1000-player capacity until repeatable networked workload benchmarks demonstrate it.
6. Keep the `test` branch as the active implementation line; do not silently change `main`.
7. Chronos must preserve observable game semantics; throughput gains that break movement, combat, ordering or persistence do not count as improvements.
