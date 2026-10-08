# Grimholt forensic repository and parity audit

- Audit baseline: 2026-10-09
- Repository: `HazelTheSquirrel/Grimholt`
- Audited ref: `test` at `fd4470f761574a25bf8756c1c9061925e146065b`
- Target: Minecraft Java Edition 26.4 Snapshot 3
- Reference SHA-1: `2d89c95c030e635387448f332961074ce1adbb4b`

## Executive conclusion

Grimholt has meaningful server foundations but is **not feature-complete and not yet proven compatible with a real 26.4 Snapshot 3 client**. This audit is an evidence-based implementation backlog, not a claim that every listed subsystem has been re-tested during this documentation pass.

The repository tree contains a Java/Gradle server, public Grimholt API, server kernel and vanilla packages, unit tests, pinned reference JARs for 26.2 and 26.4, generated-reference tasks, a standalone-JAR task, dependency audit and CI. Current target is 26.4 Snapshot 3. The old 26.2 artifacts/docs must not be mistaken for the active target.

## Confirmed from repository/build configuration

- Java 25 and Gradle 9.8.0 are configured.
- The 26.4 Snapshot 3 JAR is checked in at `reference/minecraft/26.4/server.jar`; the build/CI pin its SHA-1.
- The Gradle runtime dependency declaration contains SLF4J API/runtime implementation and tests; it does not declare `net.minestom:minestom`.
- A dependency audit blocks selected Minestom/Bukkit/Spigot/Paper/Folia coordinates.
- A standalone JAR task packages the application and runtime classpath dependencies.
- CI checks the reference checksum, runs Java tests, boots the official reference server, assembles and uploads the standalone artifact.
- Native Grimholt connection code has a strict handshake decoder and tests for valid status/login intent and malformed/trailing/mismatched inputs.
- Resource-budget, persistence-path/atomic-file and other kernel tests have been added in prior commits.

## Evidence limitations

The GitHub CI run associated with the audited head was in progress at audit time: test step had passed, while the Mojang reference smoke test was still running. Therefore this audit does not call that commit's entire CI green. Re-check the run linked from the commit before merging.

This pass reviewed repository tree and key documentation/build configuration. It did not execute a local clean build, connect a live Minecraft client, run packet fuzzing, generate a full differential report, or benchmark a networked server. Those are explicitly required work items, not implied accomplishments.

## Domain findings

### 1. Protocol and client compatibility — PARTIAL

Handshake validation is stricter, but a valid handshake is only the first protocol state. Required: status response/ping parity; all Login and Encryption/Compression paths; session verification; Configuration packets and registry NBT; cookies/resource-pack/transfer semantics where supported; Play packet coverage; packet size/rate limits; strict string/NBT/VarInt parsing; disconnect and timeout behavior; target-client tests.

### 2. Player and world runtime — PARTIAL

Grimholt-owned abstractions exist for player state, regions/world/chunks and packet serialization. Verify that the live socket/player/chunk/entity lifecycle uses them consistently, with no parallel state becoming authoritative. Required: real-client spawn, movement, teleport acknowledgements, chunk borders, tracking, reconnect and orderly disconnect.

### 3. Gameplay — INCOMPLETE

A handful of domain models/engines do not equal vanilla mechanics. The work order must cover collision/physics; placement/breaking; scheduled/random ticks; fluids; redstone; block entities; item components and inventories; menus/crafting; entities/AI/pathfinding; damage/combat/projectiles; drops/loot; villagers/POIs/trading/raids; dimensions/portals/bosses; commands/gamerules/scoreboards.

### 4. Data and world generation — INCOMPLETE

The generator can launch Mojang's data generator and write a manifest/tag index, but that does not prove all generated reports are consumed correctly by runtime systems. Required: complete registry/tag/component/recipe/loot/advancement/predicate/function/datapack support; exact seeded noise/biome/surface/carver/feature/structure generation; lighting; spawn placement; snapshot-specific content; differential tests.

### 5. Persistence — PARTIAL / UNPROVEN

Atomic-file/path-safety primitives are useful but do not prove complete Anvil parity. Required: level metadata, chunks/sections/heightmaps/light, block entities, entities, player data, POI, raids, maps, statistics/advancements, version migration, atomicity across related files, corrupt-file recovery, interrupted-save tests and restart round trips.

### 6. Concurrency and performance — PARTIAL

Resource profiling and region/handoff abstractions exist. Required: explicit ownership for all mutable state, bounded queues/backpressure, queue-saturation behavior, deterministic cross-region ordering, shutdown cancellation/draining, races under join/quit/chunk unload, tick watchdog and observability. A logical world-model microbenchmark is not a 500–1000 player network benchmark.

### 7. Plugin API and operations — FOUNDATION

Public API separation and plugin lifecycle foundations exist. Required: API versioning/compatibility, world/item/inventory/block/entity capabilities, permissions, region-aware scheduler contract, listener/task cleanup, plugin quotas/telemetry, classloader leak testing, configuration migration, structured logging and release operations. Plugins are trusted code, not a security sandbox.

## Prioritized blocker order

1. Keep CI green and establish reproducible baseline.
2. Prove protocol/client compatibility through status → login → configuration → play with a real 26.4 client.
3. Make the player/world/chunk lifecycle authoritative and end-to-end.
4. Implement basic survival loop: movement/collision, block break/place, drops, inventory and persistence.
5. Implement tick/neighbor/block-entity/fluid/redstone foundations.
6. Implement items/containers/crafting and entity lifecycle/AI/combat.
7. Implement dimensions, portals, villagers/raids and remaining vanilla systems.
8. Complete generated data/datapack/command/registry semantics and exact world generation/lighting.
9. Differential, fuzz, concurrency, crash-recovery and long-soak testing.
10. Measure 50/100/250/500/750/1000-player networked workloads before any capacity claim.

The detailed, dependency-ordered work and release gates live in `MASTER-WORKPLAN.md`. Update this audit only when code/test evidence changes the status.
