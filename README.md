# Grimholt

Grimholt is an independently implemented Minecraft server fork. Its runtime is owned by Grimholt; it must not require Minestom, Paper, Purpur, Spigot, Bukkit, Folia, or Velocity as a server implementation.

## Strategic goal

The highest-level goal is to build **Grimholt as an independent server implementation**, not as a plugin, proxy, wrapper, or application that permanently depends on an external Minecraft server runtime.

The intended relationship is:

Minecraft client -> Grimholt runtime -> Grimholt protocol/world/entity/tick/plugin systems

Grimholt must own its runtime behavior and must not depend on Minestom source packages or an externally released Minestom artifact to function. Mojang's official server JAR is used only as a version-pinned behavioral/reference target and is not bundled into Grimholt's runtime.

This is similar in spirit to established Minecraft server fork ecosystems: Grimholt has its own identity, API, lifecycle, implementation decisions, performance model and release/update process.

## Goals

- Vanilla Minecraft gameplay and protocol compatibility for the targeted Minecraft release.
- No Bukkit, Spigot, Paper or Folia runtime/API dependency.
- A first-class, stable Grimholt Plugin API.
- Strong separation between server internals and public plugin API.
- Architecture designed for automatic CPU/core utilization, partitioned multithreading and high concurrency.
- Performance target: 500-1000 concurrent players, subject to hardware, world activity and gameplay workload.
- Automatic host-resource profiling with an 80% physical-memory server budget and 20% system/safety reserve.
- Adaptive runtime control with a deterministic safety layer and a future optional Grimholt KI/ML controller.

## Current status

Grimholt is currently in the **transition from Minestom-based implementation to source-fork architecture**.

The repository contains a Grimholt-owned server kernel, world model, vanilla systems, plugin API, native command/network/connection layers, and version-pinned Minecraft 26.4 Snapshot 3 reference tooling. Reference tooling and partial gameplay systems do not imply complete vanilla parity. The build no longer consumes `net.minestom:minestom`.

The fork migration has now crossed the runtime boundary: Grimholt owns command dispatch, network connections, packet framing, player lifecycle, world/chunk transport, entity lifecycle and region ticking. The external Minestom runtime dependency has been removed from Gradle and the old Minestom adapter/package has been deleted.

**Minecraft parity is not complete yet.** The current reference target is Minecraft Java 26.4 Snapshot 3 (pinned Mojang server JAR SHA-1 `2d89c95c030e635387448f332961074ce1adbb4b`, Java 25). The runtime protocol/gameplay implementation is still incomplete and must be migrated and verified against that snapshot before compatibility can be claimed.

## Engineering rule

Never declare a phase complete merely because the code compiles. Each phase must pass:
1. implementation checks,
2. API/architecture review,
3. concurrency/performance review where applicable,
4. regression tests,
5. dependency audit,
6. a second adversarial review focused on failure modes.

Before every implementation change, follow `docs/MASTER-WORKPLAN.md`. It is the mandatory pre-change and post-change engineering checklist.

See `docs/MASTER-WORKPLAN.md`, `docs/COMPATIBILITY.md`, `docs/ARCHITECTURE.md` and `docs/PERFORMANCE.md`.





## Versioning rule

Minecraft and the Grimholt/Minestom fork baseline are intentionally managed as controlled, explicit updates. A Minecraft update changes the pinned `VanillaSnapshot` and regenerated reference data. Grimholt-owned implementation changes are integrated deliberately rather than being silently inherited from a Maven version. Grimholt may develop against Minecraft releases and snapshots independently of Minestom's release cadence.

The exact Mojang reference currently pinned is **Minecraft Java 26.4 Snapshot 3**. Generated Mojang reports are reference input, not a replacement for Grimholt-owned behavior; protocol IDs, data versions, gameplay, persistence, and client interoperability must be validated independently.

See `docs/MASTER-WORKPLAN.md`, `docs/ARCHITECTURE.md`, `docs/PERFORMANCE.md`, `docs/VERSION-UPDATE.md` and issue #1 for the strategic fork migration.

**Fork migration work is currently performed on the `test` branch.**