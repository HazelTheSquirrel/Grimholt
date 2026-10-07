# Grimholt — Master Work Plan

> **Purpose:** This document is the mandatory engineering control document for Grimholt.
>
> Before **every implementation change**, the current phase, this plan, the architecture, the dependency policy and the relevant existing code must be reviewed. After the change, the same checklist is run again as an adversarial second pass.
>
> **Repository rule:** For this fork migration, implementation work is performed on the `test` branch. `main` remains the stable baseline until a deliberate merge.
>
> **Project goal:** Build an independent Minecraft server implementation/distribution based on Minestom, with vanilla Minecraft compatibility for the targeted version and a first-class Grimholt Plugin API. Bukkit, Spigot, Paper and Folia are explicitly excluded.

---

## 0. Non-negotiable project rules

These rules apply to every phase.

### 0.1 Platform boundaries

- [ ] No Bukkit dependency.
- [ ] No Spigot dependency.
- [ ] No Paper dependency.
- [ ] No Folia dependency.
- [ ] No compatibility layer that requires those APIs at runtime.
- [ ] Minestom may be used internally as the implementation foundation.
- [ ] Minestom implementation classes must not become the Grimholt public plugin API.
- [ ] Plugins compile against Grimholt API, not against internal server classes.

### 0.2 Branch and repository rules

- [ ] For this fork migration, implementation work is performed on the `test` branch. `main` remains the stable baseline until a deliberate merge.
- [ ] Never create a feature branch for implementation unless the user explicitly changes this rule.
- [ ] Do not silently rewrite unrelated files.
- [ ] Every logical change has a clear commit message.
- [ ] Before writing, verify the current `main` state.
- [ ] After writing, re-fetch the changed files and verify their actual repository contents.

### 0.3 Engineering rules

- [ ] Do not treat compilation as proof of correctness.
- [ ] Do not claim Minecraft compatibility without testing evidence.
- [ ] Do not claim 500–1000 players as a guaranteed capacity.
- [ ] Design the architecture so 500 and 1000-player workloads can be benchmarked without redesigning ownership boundaries.
- [ ] Avoid global mutable state unless ownership and lifecycle are explicit.
- [ ] Avoid unbounded queues.
- [ ] Avoid giant global locks.
- [ ] Keep latency-sensitive tick work separate from heavy asynchronous work.
- [ ] Every public API addition receives an API compatibility review.
- [ ] Every new dependency receives a dependency review.
- [ ] Every subsystem receives a concurrency/ownership review.
- [ ] Every completed phase receives a separate adversarial review.

---

# 1. Mandatory pre-change procedure

This procedure must be followed **before every meaningful implementation session or change**.

## 1.1 Repository preflight

- [ ] Confirm repository is `HazelTheSquirrel/Grimholt`.
- [ ] Confirm current branch/ref is `main`.
- [ ] Inspect current repository tree.
- [ ] Read the current phase status.
- [ ] Read this Master Work Plan.
- [ ] Read `docs/ARCHITECTURE.md`.
- [ ] Read `docs/DEPENDENCY-POLICY.md`.
- [ ] Read `docs/COMPATIBILITY.md` and the relevant phase sections in this plan.
- [ ] Inspect all existing files directly related to the planned change.
- [ ] Check recent commits for changes that may affect the task.

## 1.2 Upstream/API preflight

When Minestom or another dependency is involved:

- [ ] Inspect the current upstream API/source actually used by the repository.
- [ ] Verify class names, methods, constructors and lifecycle semantics.
- [ ] Do not rely on remembered APIs.
- [ ] Check version compatibility.
- [ ] Check whether the required behavior belongs in Grimholt or should remain behind the implementation boundary.
- [ ] Check licensing requirements before copying or vendoring source.

## 1.3 Design preflight

Before writing code, answer:

1. What problem is being solved?
2. Which layer owns the responsibility?
3. What state does it own?
4. Which thread/executor owns that state?
5. What happens during shutdown?
6. What happens if the operation fails halfway through?
7. Can the operation block the tick path?
8. Can it create an unbounded queue or collection?
9. Can a plugin obtain internal implementation references?
10. Does the change lock us into a bad public API?
11. Does it introduce a dependency?
12. How will it be tested?
13. How will it be stress-tested?
14. How will it be reviewed again after implementation?

If any answer is unknown and materially affects correctness, **stop implementation and resolve the unknown first**.

---

# 2. Mandatory post-change procedure

After implementation:

## 2.1 Functional review

- [ ] Re-read every changed file.
- [ ] Check imports and dependency boundaries.
- [ ] Check lifecycle ownership.
- [ ] Check null/error handling.
- [ ] Check shutdown behavior.
- [ ] Check resource cleanup.
- [ ] Check persistence implications.
- [ ] Check public API exposure.

## 2.2 Concurrency review

- [ ] Identify every shared mutable state introduced.
- [ ] Identify its owner.
- [ ] Identify every thread that can access it.
- [ ] Check races during startup.
- [ ] Check races during normal operation.
- [ ] Check races during shutdown.
- [ ] Check cancellation.
- [ ] Check queue bounds/backpressure.
- [ ] Check deadlock potential.
- [ ] Check starvation potential.
- [ ] Check executor leakage.

## 2.3 Dependency review

- [ ] Search direct dependencies.
- [ ] Search transitive dependency implications where possible.
- [ ] Confirm no Bukkit/Spigot/Paper/Folia dependency.
- [ ] Confirm licenses.
- [ ] Confirm the dependency is actually necessary.
- [ ] Confirm it does not accidentally become public API.

## 2.4 Adversarial second pass

Pretend the implementation is broken and actively search for:

- startup failure,
- partial initialization,
- repeated start/stop,
- abrupt disconnects,
- malformed input,
- invalid configuration,
- duplicate registration,
- dependency cycles,
- callback exceptions,
- resource leaks,
- class-loader leaks,
- race conditions,
- queue explosions,
- memory growth,
- tick stalls,
- deadlocks,
- corrupted persistence,
- incompatible API assumptions,
- hidden Minestom leakage,
- hidden Bukkit-style assumptions.

Only after this second pass may the work be considered ready for the next task.

---

# 3. Phase 0 — Project foundation and engineering controls

**Status:** Completed.

## Goals

Establish the rules that prevent the project from drifting into a Bukkit/Paper-style architecture.

## Deliverables

- [x] Repository foundation.
- [x] Architecture document.
- [x] Dependency policy.
- [x] Initial roadmap.
- [x] Performance target.
- [x] Definition of done.
- [x] Adversarial review policy.
- [x] Main-branch-only rule.

## Exit criteria

- [x] Public/internal boundary defined.
- [x] Dependency bans documented.
- [x] 500–1000 player target documented as an engineering target, not a guarantee.
- [x] Mandatory second-review process documented.

---

# 4. Phase 1 — Build system and bootstrap

## Goal

Create a reproducible Grimholt executable foundation that can start, expose lifecycle state, load configuration and shut down cleanly.

## 4.1 Build foundation

- [ ] Establish Gradle project structure.
- [ ] Establish Java toolchain/version.
- [ ] Pin dependency versions.
- [ ] Configure reproducible builds where practical.
- [ ] Add unit-test infrastructure.
- [ ] Add integration-test infrastructure.
- [ ] Add static analysis where justified.
- [ ] Add formatting/checking rules where justified.
- [ ] Document build commands.

## 4.2 Grimholt bootstrap

- [ ] Create the Grimholt application entry point.
- [ ] Define startup sequence.
- [ ] Define shutdown sequence.
- [ ] Define startup failure behavior.
- [ ] Define exit codes.
- [ ] Define ownership of created resources.
- [ ] Prevent accidental double-start.
- [ ] Prevent unsafe double-shutdown.

## 4.3 Configuration

- [ ] Define configuration model.
- [ ] Define defaults.
- [ ] Define validation.
- [ ] Define invalid-value behavior.
- [ ] Define configuration file format.
- [ ] Define future migration/versioning strategy.
- [ ] Avoid leaking configuration implementation into plugin API.

## 4.4 Logging

- [ ] Establish structured logging boundary.
- [ ] Define startup/shutdown logging.
- [ ] Define error logging.
- [ ] Avoid excessive per-tick logging.
- [ ] Avoid sensitive data leakage.
- [ ] Make plugin logging identifiable.

## 4.5 Lifecycle state machine

Define explicit states, for example:

`NEW → STARTING → RUNNING → STOPPING → STOPPED`

and deterministic failure behavior.

- [ ] Define legal transitions.
- [ ] Reject illegal transitions.
- [ ] Ensure startup failure reaches a terminal state.
- [ ] Ensure shutdown is idempotent.
- [ ] Ensure all owned executors/resources are closed.

## 4.6 Minestom integration boundary

- [ ] Integrate the current supported Minestom version/API.
- [ ] Keep Minestom behind the implementation boundary.
- [ ] Do not expose Minestom types from Grimholt API.
- [ ] Define the first adapter/wrapper layer.
- [ ] Document which Minestom functionality is currently relied upon.

## 4.7 Exit gate

- [ ] Fresh checkout can build.
- [ ] Server starts.
- [ ] Server shuts down.
- [ ] Invalid configuration fails deterministically.
- [ ] Repeated start/stop is safe.
- [ ] No banned dependency exists.
- [ ] No public API leaks Minestom internals.
- [ ] Tests cover lifecycle failures.

## Adversarial tests

- [ ] Start twice.
- [ ] Stop twice.
- [ ] Start with malformed configuration.
- [ ] Fail during initialization.
- [ ] Shutdown while initialization is pending.
- [ ] Shutdown while asynchronous work exists.
- [ ] Plugin discovery failure.
- [ ] Invalid plugin metadata.
- [ ] Missing configuration file.
- [ ] Permission/file-system failure.

---

# 5. Phase 2 — Grimholt Plugin API

## Goal

Create a real independent plugin platform rather than exposing Minestom's internal/plugin system as the public API.

## 5.1 Plugin identity

- [ ] Define plugin ID rules.
- [ ] Define plugin name/version.
- [ ] Define authors/metadata.
- [ ] Define minimum Grimholt API version.
- [ ] Define plugin descriptor format.
- [ ] Validate descriptor schema.

## 5.2 Plugin class loading

- [ ] Define plugin class-loader boundary.
- [ ] Define parent/child loading rules.
- [ ] Define API visibility.
- [ ] Define library isolation rules.
- [ ] Define shutdown/unload behavior.
- [ ] Prevent class-loader leaks.

## 5.3 Dependency resolution

- [ ] Define required dependencies.
- [ ] Define optional dependencies.
- [ ] Define load ordering.
- [ ] Detect duplicate IDs.
- [ ] Detect missing dependencies.
- [ ] Detect dependency cycles.
- [ ] Define deterministic error reporting.

## 5.4 Lifecycle

- [ ] Define load.
- [ ] Define enable.
- [ ] Define disable.
- [ ] Define failure behavior.
- [ ] Define partial-load cleanup.
- [ ] Define server-shutdown interaction.

## 5.5 Events

- [ ] Define event contracts.
- [ ] Define listener registration.
- [ ] Define listener removal.
- [ ] Define ordering rules.
- [ ] Define cancellation semantics.
- [ ] Define exception isolation.
- [ ] Define threading semantics.

## 5.6 Commands

- [ ] Define command API.
- [ ] Define command registration.
- [ ] Define argument parsing.
- [ ] Define permissions.
- [ ] Define tab completion.
- [ ] Define command lifecycle.
- [ ] Prevent plugin commands from bypassing server ownership.

## 5.7 Scheduler

- [ ] Define public scheduler API.
- [ ] Separate tick-thread work from asynchronous work.
- [ ] Define delayed tasks.
- [ ] Define repeating tasks.
- [ ] Define cancellation.
- [ ] Define ownership.
- [ ] Define shutdown behavior.
- [ ] Define bounded execution where applicable.

## 5.8 Services/registries

- [ ] Define service registration.
- [ ] Define service lookup.
- [ ] Define ownership.
- [ ] Define replacement rules.
- [ ] Define shutdown cleanup.

## 5.9 Exit gate

- [ ] Third-party plugin compiles against Grimholt API only.
- [ ] No Minestom implementation type is required.
- [ ] Plugin failures are isolated.
- [ ] Dependency resolution is deterministic.
- [ ] Plugin shutdown is deterministic.
- [ ] API documentation exists.

## Adversarial tests

- [ ] Malformed descriptor.
- [ ] Duplicate plugin ID.
- [ ] Missing dependency.
- [ ] Dependency cycle.
- [ ] Plugin throws during load.
- [ ] Plugin throws during enable.
- [ ] Plugin throws during disable.
- [ ] Plugin schedules endless work.
- [ ] Plugin floods events.
- [ ] Plugin retains server objects after unload.
- [ ] Plugin class-loader leak test.

---

# 6. Phase 3 — Minecraft connection and vanilla server kernel

## Goal

Reach the point where a normal targeted-version Minecraft client can connect and participate in ordinary server gameplay.

## 6.1 Network/login

- [ ] Protocol version handling.
- [ ] Handshake.
- [ ] Status/ping.
- [ ] Login.
- [ ] Authentication/online-mode boundary.
- [ ] Encryption/session handling where required.
- [ ] Compression.
- [ ] Configuration phase.
- [ ] Play phase.
- [ ] Disconnect handling.
- [ ] Timeout handling.

## 6.2 Player model

- [ ] Player identity.
- [ ] Position/rotation.
- [ ] Game mode.
- [ ] Health.
- [ ] Hunger.
- [ ] Experience.
- [ ] Inventory.
- [ ] Abilities.
- [ ] Effects.
- [ ] Respawn.
- [ ] Death.
- [ ] Client synchronization.

## 6.3 World/instance model

- [ ] World ownership.
- [ ] Dimension identity.
- [ ] Spawn.
- [ ] Time.
- [ ] Weather.
- [ ] Difficulty.
- [ ] World border.
- [ ] Chunk lifecycle.
- [ ] View/simulation distances.
- [ ] World unload.

## 6.4 Blocks

- [ ] Block registry.
- [ ] Block states.
- [ ] Placement.
- [ ] Breaking.
- [ ] Block updates.
- [ ] Neighbor updates.
- [ ] Block entity boundary.

## 6.5 Items/inventories

- [ ] Item registry.
- [ ] Item stacks.
- [ ] Components/data.
- [ ] Inventory synchronization.
- [ ] Container opening.
- [ ] Container closing.
- [ ] Item pickup/drop.
- [ ] Item interaction.

## 6.6 Entities

- [ ] Entity identity.
- [ ] Spawn/despawn.
- [ ] Position synchronization.
- [ ] Collision boundary.
- [ ] Entity ticking.
- [ ] Entity removal.
- [ ] Player/entity tracking.

## 6.7 Persistence

- [ ] World save.
- [ ] Chunk save.
- [ ] Player save.
- [ ] Server state save where applicable.
- [ ] Crash-safe boundaries.
- [ ] Load/save round-trip tests.

## Exit gate

- [ ] Vanilla client connects.
- [ ] Player can move.
- [ ] Blocks can be broken/placed.
- [ ] Inventory works.
- [ ] Chunks load/unload.
- [ ] Player can disconnect/reconnect.
- [ ] World survives restart.
- [ ] Player state survives restart.

## Adversarial tests

- [ ] Disconnect during login.
- [ ] Disconnect during chunk loading.
- [ ] Disconnect during teleport.
- [ ] Malformed packets.
- [ ] Packet floods.
- [ ] Chunk load/unload race.
- [ ] Entity removal during iteration.
- [ ] Save during shutdown.
- [ ] Interrupted save.
- [ ] Corrupted persistence input.

---

# 7. Phase 4 — Vanilla gameplay/mechanics parity

## Goal

Implement and verify actual Minecraft gameplay rather than merely protocol compatibility.

## 7.1 Movement and physics

- [ ] Collision.
- [ ] Gravity.
- [ ] Falling.
- [ ] Jumping.
- [ ] Swimming.
- [ ] Climbing.
- [ ] Flying/creative movement.
- [ ] Knockback.
- [ ] Movement validation.

## 7.2 Fluids

- [ ] Water.
- [ ] Lava.
- [ ] Flow.
- [ ] Source behavior.
- [ ] Fluid interactions.
- [ ] Fluid update scheduling.

## 7.3 Block updates

- [ ] Neighbor updates.
- [ ] Scheduled ticks.
- [ ] Random ticks.
- [ ] Block state transitions.
- [ ] Growth.
- [ ] Decay.
- [ ] Fire.

## 7.4 Redstone

- [ ] Power levels.
- [ ] Dust.
- [ ] Torches.
- [ ] Repeaters.
- [ ] Comparators.
- [ ] Pistons.
- [ ] Observers.
- [ ] Redstone components.
- [ ] Update ordering.
- [ ] Quasi-connectivity where applicable.

## 7.5 Farming

- [ ] Crops.
- [ ] Bonemeal.
- [ ] Farmland.
- [ ] Mob-based farming interactions.
- [ ] Random tick behavior.

## 7.6 Containers and crafting

- [ ] Crafting.
- [ ] Furnace.
- [ ] Blast furnace.
- [ ] Smoker.
- [ ] Brewing.
- [ ] Enchanting.
- [ ] Anvil.
- [ ] Smithing.
- [ ] Other container types.
- [ ] Recipe book interactions.

## 7.7 Entities and AI

- [ ] Passive mobs.
- [ ] Hostile mobs.
- [ ] Neutral mobs.
- [ ] Aquatic mobs.
- [ ] Flying mobs.
- [ ] Villagers.
- [ ] Pathfinding.
- [ ] Target selection.
- [ ] Breeding.
- [ ] Loot behavior.
- [ ] Despawn rules.

## 7.8 Combat

- [ ] Melee.
- [ ] Critical hits.
- [ ] Shields.
- [ ] Armor.
- [ ] Damage calculation.
- [ ] Effects.
- [ ] Projectiles.
- [ ] Arrows.
- [ ] Tridents.
- [ ] Other projectile types.

## 7.9 Explosions

- [ ] TNT.
- [ ] Creepers.
- [ ] Beds/respawn anchors where applicable.
- [ ] Explosion damage.
- [ ] Block destruction.
- [ ] Chain reactions.
- [ ] Drops.

## 7.10 Villagers and raids

- [ ] Villager professions.
- [ ] POIs.
- [ ] Trading.
- [ ] Gossip/reputation behavior.
- [ ] Village mechanics.
- [ ] Raids.
- [ ] Raid spawning.
- [ ] Raid waves.
- [ ] Raid completion/failure.
- [ ] Hero of the Village.

## 7.11 Dimensions and portals

- [ ] Overworld.
- [ ] Nether.
- [ ] End.
- [ ] Portal creation.
- [ ] Portal travel.
- [ ] Dimension-specific rules.
- [ ] Respawn anchors.
- [ ] End mechanics.

## 7.12 Structures and world generation

- [ ] Terrain.
- [ ] Biomes.
- [ ] Structures.
- [ ] Features.
- [ ] Carvers.
- [ ] Caves.
- [ ] Ores.
- [ ] Trees.
- [ ] Villages.
- [ ] Strongholds.
- [ ] Nether generation.
- [ ] End generation.

## Exit gate

Every implemented mechanic has:

- [ ] Focused automated test where practical.
- [ ] Regression test for known edge cases.
- [ ] Manual/client compatibility verification where automation is insufficient.
- [ ] Performance consideration.
- [ ] Failure-mode review.

## Adversarial workloads

- [ ] Large redstone clocks.
- [ ] Piston machines.
- [ ] Massive farms.
- [ ] Entity cramming.
- [ ] Mob-heavy chunks.
- [ ] Fluid cascades.
- [ ] Explosion chains.
- [ ] Rapid block update storms.
- [ ] Chunk-border interactions.
- [ ] Dimension transition stress.

---

# 8. Phase 5 — Data, registries and protocol completeness

## Goal

Close the gaps that appear when real Minecraft clients, datapacks and complex gameplay interact with the server.

## 8.1 Registries

- [ ] Blocks.
- [ ] Items.
- [ ] Entities.
- [ ] Biomes.
- [ ] Dimensions.
- [ ] Damage types.
- [ ] Effects.
- [ ] Enchantments.
- [ ] Recipes.
- [ ] Loot.
- [ ] Other targeted-version registries.

## 8.2 NBT and data components

- [ ] NBT reading.
- [ ] NBT writing.
- [ ] Validation.
- [ ] Data components.
- [ ] Item components.
- [ ] Entity data.
- [ ] Block entity data.
- [ ] Persistence compatibility.

## 8.3 Gameplay data

- [ ] Recipes.
- [ ] Loot tables.
- [ ] Tags.
- [ ] Advancements.
- [ ] Statistics.
- [ ] Scoreboards.
- [ ] Teams.
- [ ] Boss bars.
- [ ] Resource packs.
- [ ] Datapack-facing interfaces.
- [ ] Commands and command tree synchronization.

## 8.4 Protocol edge cases

- [ ] Configuration/play transitions.
- [ ] Unknown/unsupported packets.
- [ ] Packet ordering.
- [ ] Fragmentation/size limits.
- [ ] Compression boundaries.
- [ ] Registry synchronization.
- [ ] Client capability differences.
- [ ] Disconnect reason correctness.

## Exit gate

- [ ] Target-version compatibility matrix exists.
- [ ] Registry synchronization verified.
- [ ] Data-component behavior verified.
- [ ] Datapack-facing behavior documented.
- [ ] Protocol edge cases have regression coverage.

## Adversarial tests

- [ ] Malformed NBT.
- [ ] Oversized payloads.
- [ ] Invalid registry entries.
- [ ] Unknown IDs.
- [ ] Invalid component combinations.
- [ ] Broken recipes.
- [ ] Broken loot tables.
- [ ] Reload failure.
- [ ] Client disconnect during data synchronization.

---

# 9. Phase 6 — Performance and high-concurrency architecture

## Goal

Make the server measurable and architecturally capable of serious concurrency.

**Important:** 500–1000 concurrent players is a workload target. It is not a universal hardware guarantee.

## 9.1 Measurement infrastructure

- [ ] Tick duration metrics.
- [ ] Tick overrun metrics.
- [ ] CPU metrics.
- [ ] Heap metrics.
- [ ] Allocation metrics.
- [ ] GC metrics.
- [ ] Network metrics.
- [ ] Chunk metrics.
- [ ] Entity metrics.
- [ ] Plugin metrics.
- [ ] Queue depth metrics.

## 9.2 Work scheduling

- [ ] Separate tick-critical work from background work.
- [ ] Bounded worker pools.
- [ ] Bounded queues.
- [ ] Backpressure.
- [ ] Cancellation.
- [ ] Shutdown draining rules.
- [ ] Work ownership documentation.

## 9.3 Chunk pipeline

- [ ] Async generation.
- [ ] Async loading.
- [ ] Async saving.
- [ ] Chunk ticket management.
- [ ] View-distance pressure.
- [ ] Generation throttling.
- [ ] Storage backpressure.

## 9.4 Player scaling

Benchmark at minimum:

- [ ] 50 players.
- [ ] 100 players.
- [ ] 250 players.
- [ ] 500 players.
- [ ] 750 players.
- [ ] 1000 players.

Workloads:

- [ ] Idle.
- [ ] Normal exploration.
- [ ] High movement.
- [ ] Chunk generation.
- [ ] Dense entities.
- [ ] Redstone.
- [ ] Mass block updates.
- [ ] Combat.
- [ ] Network burst.
- [ ] Join/quit storm.

## 9.5 Plugin performance

- [ ] Plugin execution metrics.
- [ ] Slow listener detection.
- [ ] Slow command detection.
- [ ] Scheduler abuse detection.
- [ ] Per-plugin resource visibility.
- [ ] Safe failure behavior.

## 9.6 Memory

- [ ] Player memory accounting.
- [ ] Chunk memory accounting.
- [ ] Entity memory accounting.
- [ ] Plugin memory considerations.
- [ ] Cache bounds.
- [ ] Leak detection.
- [ ] Class-loader leak detection.

## Exit gate

- [ ] Baseline benchmark suite exists.
- [ ] Regression thresholds are documented.
- [ ] No architectural redesign is required between 500 and 1000-player benchmark configurations.
- [ ] Known bottlenecks are documented.
- [ ] Performance claims are backed by measurements.

## Adversarial tests

- [ ] 1000 active players.
- [ ] 1000-player chunk generation.
- [ ] Entity-heavy worlds.
- [ ] Redstone-heavy worlds.
- [ ] Network bursts.
- [ ] Join/quit storms.
- [ ] Plugin abuse.
- [ ] Memory pressure.
- [ ] Long-duration soak tests.

---

# 10. Phase 7 — Production hardening

## Goal

Make failures diagnosable and prevent avoidable world/data corruption.

## 10.1 Reliability

- [ ] Crash handling.
- [ ] Controlled shutdown.
- [ ] Watchdog/health reporting.
- [ ] Deadlock detection where practical.
- [ ] Startup diagnostics.
- [ ] Recovery diagnostics.

## 10.2 Configuration

- [ ] Full validation.
- [ ] Human-readable errors.
- [ ] Versioned configuration.
- [ ] Migration tooling.
- [ ] Safe defaults.
- [ ] Unknown-field handling.

## 10.3 Persistence safety

- [ ] Atomic save boundaries where possible.
- [ ] Backup strategy.
- [ ] Recovery strategy.
- [ ] Corruption detection.
- [ ] Migration tests.
- [ ] Interrupted-write tests.

## 10.4 Security

- [ ] Network input validation.
- [ ] Packet abuse protection.
- [ ] Resource exhaustion protection.
- [ ] Plugin trust model.
- [ ] File-system access review.
- [ ] Command permission review.
- [ ] Dependency vulnerability review.

## 10.5 Operations

- [ ] Health status.
- [ ] Metrics export strategy.
- [ ] Log rotation strategy.
- [ ] Crash report strategy.
- [ ] Diagnostic commands.
- [ ] Runtime configuration documentation.

## Exit gate

- [ ] Controlled failures are diagnosable.
- [ ] World data is protected against expected failure modes.
- [ ] Security review is documented.
- [ ] Operational documentation exists.

---

# 11. Phase 8 — Release candidate

## Goal

Produce a defensible production candidate.

## Deliverables

- [ ] Complete compatibility matrix.
- [ ] Complete performance report.
- [ ] API documentation.
- [ ] Example plugin.
- [ ] Plugin development guide.
- [ ] Server administration guide.
- [ ] Upgrade/migration guide.
- [ ] Reproducible release build.
- [ ] Dependency/license report.
- [ ] Known limitations.
- [ ] Known incompatibilities.
- [ ] Benchmark methodology.
- [ ] Recovery documentation.

## Final audit

- [ ] No Bukkit dependency.
- [ ] No Spigot dependency.
- [ ] No Paper dependency.
- [ ] No Folia dependency.
- [ ] No accidental public Minestom implementation API.
- [ ] No unresolved critical concurrency issue.
- [ ] No unresolved critical persistence issue.
- [ ] No unresolved critical security issue.
- [ ] API compatibility review completed.
- [ ] Dependency audit completed.
- [ ] Performance audit completed.
- [ ] Adversarial review completed.

## Final release gate

Grimholt is only considered a production candidate when every critical item above is either complete or explicitly documented as a known, accepted limitation.

---

# 12. Cross-phase test matrix

This matrix is maintained throughout the project.

## Build

- [ ] Clean checkout build.
- [ ] Clean test run.
- [ ] Reproducibility check.
- [ ] Dependency graph check.

## Lifecycle

- [ ] Start.
- [ ] Stop.
- [ ] Restart.
- [ ] Failed start.
- [ ] Failed shutdown.
- [ ] Repeated lifecycle operations.

## Networking

- [ ] Valid client.
- [ ] Invalid client input.
- [ ] Disconnect.
- [ ] Timeout.
- [ ] Packet flood.
- [ ] Large payload.

## World

- [ ] Create.
- [ ] Load.
- [ ] Save.
- [ ] Unload.
- [ ] Reload.
- [ ] Corruption handling.

## Plugins

- [ ] Load.
- [ ] Enable.
- [ ] Disable.
- [ ] Failure.
- [ ] Dependency cycle.
- [ ] Class-loader cleanup.

## Concurrency

- [ ] Startup race.
- [ ] Shutdown race.
- [ ] Player disconnect race.
- [ ] Chunk load/unload race.
- [ ] Entity lifecycle race.
- [ ] Async task cancellation.
- [ ] Queue saturation.

## Performance

- [ ] Baseline.
- [ ] Regression.
- [ ] Stress.
- [ ] Soak.
- [ ] Memory.
- [ ] Allocation.
- [ ] Network.

---

# 13. Mandatory phase completion template

Every phase must be evaluated using this checklist.

### Implementation

- [ ] All phase deliverables implemented.
- [ ] No known unfinished critical path.
- [ ] Documentation updated.

### Tests

- [ ] Unit tests.
- [ ] Integration tests.
- [ ] Regression tests.
- [ ] Stress tests where applicable.

### Architecture

- [ ] Ownership reviewed.
- [ ] Threading reviewed.
- [ ] Public API reviewed.
- [ ] Persistence reviewed.
- [ ] Dependency boundary reviewed.

### Adversarial second pass

- [ ] Failure cases deliberately attacked.
- [ ] Race conditions considered.
- [ ] Resource leaks considered.
- [ ] Performance regressions considered.
- [ ] Security implications considered.
- [ ] Hidden coupling searched for.

### Final decision

- [ ] PASS — phase may advance.
- [ ] BLOCKED — defect/unknown must be resolved first.

---

# 14. Change log

| Date | Phase | Change | Result |
|---|---|---|---|
| 2026-10-07 | 0 | Master engineering controls established | Initial version |
| 2026-10-07 | 1 | Build/bootstrap foundation implemented; adversarial review completed | BLOCKED pending actual build/test execution |

---

# 15. Golden rule

**Before changing Grimholt, read this plan.**

**Before declaring a change finished, attack the change as if it were broken.**

**If the architecture, API boundary, concurrency model, dependency boundary or failure behavior is unclear, do not hide the uncertainty with code. Resolve it first.**


---
# 16. Current audit baseline — 2026-10-07

This section supersedes historical phase-review documents. The repository deliberately keeps this plan as a living control document; completed review snapshots are deleted instead of retained as parallel sources of truth.

## Proven foundation

- CI has executed successfully on main with JDK 25 and Gradle 9.8.0.
- Minestom 26.2 is an internal implementation dependency only.
- The public Grimholt API exposes neither Minestom nor SLF4J types.
- Plugin discovery, dependency ordering, lifecycle and classloader cleanup exist.
- The server can select online or offline authentication.
- The server configures Minestom dispatcher thread count instead of accepting its single-thread default.
- Anvil persistence is wired through the 26.2 dimension-aware loader.

## Explicitly incomplete

- Full vanilla 26.2 world generation.
- Full vanilla mechanics: physics, fluids, redstone, block ticks, inventories, crafting, combat, entities, AI, villagers, raids, dimensions and portals.
- Complete vanilla data, command and datapack behavior.
- End-to-end online authentication/client compatibility testing.
- 500–1000 player benchmark evidence.

## Next implementation priority

1. Establish the vanilla behavior/reference test harness.
2. Build the Grimholt-owned world/chunk and region ownership model around Minestom's partitioned tick execution.
   - Logical region keys and bounded explicit cross-owner handoffs are now implemented.
   - Minestom remains unchanged and is still consumed only as the execution substrate.
3. Implement vanilla player movement, block interaction and persistence round-trips.
4. Implement mechanics in dependency order and test every subsystem against 26.2 behavior.
5. Expand the plugin API only where real vanilla capabilities require a stable public contract.
6. Benchmark only after correctness gates pass.

Paper/Folia is allowed only as an implementation-reading reference for concurrency/ownership questions. It is never a dependency, compatibility target or public API.


## 17. Snapshot/update execution rule — 2026-10-07

The active validated baseline is **Minecraft Java Edition 26.2**. Future official releases and snapshots may become Grimholt development targets as soon as their required protocol/runtime support can be implemented or integrated by Grimholt itself.

The project intentionally separates:
- **behavior/data reference:** the selected official Minecraft release/snapshot;
- **runtime/protocol substrate:** Grimholt-owned code plus Minestom-derived source during the migration.

This prevents Minestom's current version ceiling from forcing Grimholt to implement older gameplay semantics.

### Required implementation order from this point

1. Build a deterministic vanilla-reference harness and selected release/snapshot data baseline.
2. Inventory Minestom source/runtime areas that must be forked into Grimholt.
3. Establish Grimholt-owned tick ownership and cross-owner handoff primitives.
4. Replace Minestom-owned runtime boundaries with Grimholt-owned implementations incrementally.
5. Implement vanilla behavior in dependency order: player movement -> blocks/ticks/fluids -> items/containers -> entities/AI -> combat -> villagers/raids -> dimensions/portals -> world generation -> data packs/commands/registries.
6. For every subsystem, compare observable behavior against the current vanilla snapshot reference.
7. Expand the Grimholt Plugin API only after the corresponding server capability has a stable ownership contract.
8. Run adversarial concurrency tests after every subsystem.
9. Run CI after every logical change; a failed run blocks further phase advancement until repaired.
10. Only after correctness gates pass, run the 50/100/250/500/750/1000-player benchmark matrix.

### Minestom fork rule

Minestom is the source foundation, not a permanent runtime authority. During migration, Grimholt may consume the upstream artifact behind a boundary, but new core functionality must be designed so the corresponding Minestom source can be imported, owned and modified by Grimholt. The final runtime must not require the external Minestom Maven artifact.

### Update rule

Grimholt owns its Minecraft update cadence. A new release or snapshot does not wait for a third-party server fork. Required protocol, registry and runtime changes are integrated directly into Grimholt; Minestom, Paper, Folia, Purpur, Bukkit/Spigot and other projects are references where useful.

Paper/Folia remains permitted only as a temporary implementation-reading reference for concurrency ownership questions, never as a dependency or API target.

## 18. Incremental implementation checkpoint — 2026-10-07

The following concrete correctness gates are now implemented on top of the prior baseline:

- [x] Kernel-owned world models are bound to region runtimes instead of creating an unrelated world model per region.
- [x] Player lifecycle state is owned by Grimholt and survives the Minestom event boundary as a dedicated `VanillaPlayerState`.
- [x] Player position, rotation and on-ground state are captured into the Grimholt-owned player model.
- [x] Region access respects tick ownership; tests no longer read mutable region state from an unowned thread.
- [x] CI run 181 completed successfully on commit `3f10a8ce59fd30d828a816ab769fd72ad90fbbf7`.

This checkpoint does **not** change the definition of full vanilla parity. The remaining mechanics and data systems listed above are still implementation work, not documentation placeholders.
