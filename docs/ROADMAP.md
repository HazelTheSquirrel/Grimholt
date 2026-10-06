# Grimholt Engineering Roadmap

## Non-negotiable constraints

- Work only on `main`.
- No Bukkit/Spigot/Paper/Folia dependency.
- Do not expose Minestom internals directly through the public Grimholt API.
- Prefer immutable configuration and explicit lifecycle ownership.
- No global mutable singleton state unless there is a documented ownership reason.
- Public API changes require compatibility review.
- Performance claims must be measured, not assumed.

## Phase 0 — Foundation and architecture

### Deliverables
- Repository structure and engineering rules.
- Architecture boundaries.
- Plugin API boundary definition.
- Performance target and measurement policy.
- Dependency policy.

### Exit gate
- Architecture has no circular ownership model.
- Plugin API can evolve independently of server internals.
- No prohibited dependency is introduced.
- Risks for the 500-1000 player target are explicitly tracked.

### Adversarial review
Look specifically for:
- hidden Bukkit-style coupling,
- synchronous work accidentally placed on the main tick path,
- unbounded collections,
- uncontrolled plugin access to internals,
- global locks,
- API types that make future threading impossible.

## Phase 1 — Bootstrap and server lifecycle

### Deliverables
- Gradle build.
- Grimholt bootstrap.
- Configuration loading.
- Logging.
- Lifecycle state machine.
- Clean shutdown.
- Dedicated plugin discovery/loading boundary.
- First minimal integration with Minestom.

### Exit gate
- Fresh checkout builds reproducibly.
- Server starts and shuts down cleanly.
- Invalid configuration fails deterministically.
- Plugin lifecycle is deterministic.
- No Bukkit/Paper/Folia artifacts exist in the dependency graph.

### Adversarial review
Test repeated start/stop, failed startup, plugin load failure, malformed plugin metadata and shutdown while work is pending.

## Phase 2 — Grimholt Plugin API

### Deliverables
- Plugin descriptor.
- Plugin class loading.
- Dependency ordering.
- Lifecycle.
- Events.
- Commands.
- Scheduler abstraction.
- Services/registries.
- Permissions abstraction.
- Safe API surface.

### Exit gate
- A third-party plugin can be compiled against Grimholt API without server-internal classes.
- Plugin failures are isolated.
- Dependency cycles are rejected.
- Disable/unload behavior is deterministic.

### Adversarial review
Test malicious/malformed descriptors, cyclic dependencies, duplicate IDs, exceptions in callbacks, plugin starvation and class-loader leaks.

## Phase 3 — Vanilla server core

### Deliverables
- Login/play lifecycle.
- Player state.
- Worlds/instances.
- Chunks.
- Blocks/block states.
- Items/inventories.
- Entity lifecycle.
- Commands.
- Time/weather.
- Persistence.

### Exit gate
- Normal vanilla client can connect and survive ordinary gameplay.
- Save/load round trips are verified.
- Core gameplay has automated regression coverage.

### Adversarial review
Focus on chunk unload/load races, disconnect races, malformed client packets, entity removal during iteration and persistence corruption.

## Phase 4 — Vanilla mechanics parity

### Deliverables
- Physics/collision.
- Fluids.
- Redstone.
- Block updates.
- Farming.
- Containers.
- Crafting/smelting/brewing.
- Enchantments/effects.
- Villagers/trading.
- Mob behavior.
- Combat.
- Projectiles.
- Explosions.
- Raids.
- Dimensions/portals.
- Structures and world generation.

### Exit gate
Each mechanic is backed by focused tests and client-side/manual compatibility tests.

### Adversarial review
Stress chain reactions, update storms, large farms, mob density, chunk borders, dimension changes and pathological redstone/physics workloads.

## Phase 5 — Data and protocol completeness

### Deliverables
- Registries.
- NBT/data components.
- Recipes.
- Loot.
- Advancements.
- Statistics.
- Scoreboards.
- Boss bars.
- Teams.
- Resource packs.
- Datapack-facing behavior where supported.
- Protocol edge cases.

### Exit gate
Compatibility matrix exists for the targeted Minecraft version and known client interactions.

### Adversarial review
Fuzz packet boundaries, malformed NBT/data, oversized payloads, unknown registry entries and reload failures.

## Phase 6 — Performance architecture

### Deliverables
- Tick-path profiling.
- Async work queues.
- Chunk generation/storage pipeline.
- Bounded worker pools.
- Backpressure.
- Memory accounting.
- Network pressure handling.
- Per-player/per-world budgets.
- Plugin execution budgets.
- Metrics.

### Target
Architecture must be capable of being benchmarked at 500 and 1000 concurrent players without redesigning the core ownership model.

This is a target, not a promise that arbitrary hardware can sustain 1000 players.

### Adversarial review
Measure worst-case workloads, not idle players:
- 1000 players in active areas,
- chunk generation,
- entity-heavy areas,
- redstone farms,
- mass block updates,
- network bursts,
- plugin abuse,
- repeated joins/quits.

## Phase 7 — Production hardening

### Deliverables
- Crash recovery.
- Watchdogs/health reporting.
- Configuration validation.
- Operational metrics.
- Log hygiene.
- Security review.
- Plugin isolation improvements.
- Upgrade/migration tooling.
- Backup-safe persistence.

### Exit gate
A controlled failure can be diagnosed and recovered without corrupting persistent world data.

## Phase 8 — Release candidate

### Deliverables
- Compatibility matrix.
- Performance report.
- API documentation.
- Example plugins.
- Upgrade guide.
- Reproducible release build.
- Final dependency/license audit.

### Final gate
Only after all previous gates pass is Grimholt considered a production candidate.
