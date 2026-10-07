# Grimholt

Grimholt is an independent Minecraft server fork derived from the Minestom codebase.

## Strategic goal

The highest-level goal is to build **Grimholt as its own long-term Minestom fork**, not as a plugin, wrapper, or application that permanently depends on an external Minestom server runtime.

The intended relationship is:

Minecraft client -> Grimholt runtime -> Grimholt protocol/world/entity/tick/plugin systems

Minestom is the **technical starting point and source foundation**. Its code can be adopted, modified and replaced inside Grimholt as the project evolves. The final Grimholt server must own its runtime behavior and must not depend on an externally released Minestom version to function.

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

The repository already contains a Grimholt-owned server kernel, world model, vanilla systems, plugin API, version-pinned Minecraft 26.2 reference tooling and a growing parity implementation. The current build still consumes `net.minestom:minestom` as an implementation dependency. That dependency is explicitly transitional and is being removed through the fork migration on `test`.

The target is to progressively bring the required Minestom source into Grimholt, establish Grimholt ownership of the runtime, replace the remaining Minestom-owned server boundaries, and ultimately remove the external Minestom dependency.

**Minecraft parity is not complete yet.** The current behavioral target is Minecraft Java 26.2 (protocol `776`, world data version `4903`, Java 25).

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

Minecraft and the Grimholt/Minestom fork baseline are intentionally managed as controlled, explicit updates. A Minecraft update changes the pinned `VanillaSnapshot` and regenerated reference data. Minestom-derived source changes are integrated deliberately into Grimholt rather than being silently inherited from a Maven version. Grimholt may develop against Minecraft releases and snapshots independently of Minestom's release cadence.

The exact Minecraft target currently pinned is **26.2** (protocol `776`, world data version `4903`, Java 25). The generated Mojang reports are treated as reference input, not as a replacement for Grimholt-owned behavior.

See `docs/MASTER-WORKPLAN.md`, `docs/ARCHITECTURE.md`, `docs/PERFORMANCE.md`, `docs/VERSION-UPDATE.md` and issue #1 for the strategic fork migration.

**Fork migration work is currently performed on the `test` branch.**