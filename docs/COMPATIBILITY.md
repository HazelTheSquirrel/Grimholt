# Vanilla compatibility and parity

Grimholt uses two deliberately separate version anchors:

- **Vanilla behavior reference:** Minecraft Java Edition **26.2** (published 2026-10-06).
- **Current Minestom runtime/protocol foundation:** Minestom **26.2** (the exact active runtime substrate).

The snapshot is the source of truth for gameplay semantics, world rules, registries, data-driven behavior and concurrency-safe ownership requirements. Minestom is the implementation substrate only; Minestom delegation is never evidence of vanilla parity.

## Version policy

| Layer | Version | Rule |
|---|---|---|
| Vanilla behavior | 26.2 | Mandatory behavioral reference |
| Vanilla data | 26.2 | Target data/registry semantics |
| Runtime protocol | Minestom 26.2 | Temporary implementation ceiling |
| Grimholt API | Independent | Must not expose Minestom implementation types |
| Multithreading | Grimholt-owned + Minestom facilities | Never modify Minestom source |

When Minestom gains support for a newer Minecraft version, the runtime layer may advance independently. No gameplay behavior may be downgraded merely because the current Minestom runtime is older.

## Current evidence

| Area | Status | Evidence |
|---|---|---|
| 26.2 protocol transport | Implemented | Minestom 26.2 integration |
| Online/offline authentication selection | Implemented in bootstrap | Grimholt config selects online or offline authentication |
| Configurable parallel dispatcher | Implemented | Grimholt configures Minestom dispatcher threads before initialization |
| Anvil world persistence | Wired | 26.2 AnvilLoader with explicit dimension |
| Independent Grimholt plugin API | Implemented foundation | Public API contains no Minestom or SLF4J types |
| Plugin discovery/dependency/lifecycle | Implemented foundation | Descriptor validation, ordering, classloader cleanup |
| Vanilla 26.2 behavior | NOT IMPLEMENTED/PROVEN | Requires reference-driven implementation and executable parity tests |
| Vanilla world generation | NOT IMPLEMENTED/PROVEN | Requires Grimholt-owned/reference-driven generation |
| Vanilla physics/fluids/redstone | NOT IMPLEMENTED/PROVEN | Requires Grimholt-owned behavior implementation and tests |
| Vanilla entities/AI/villagers/raids | NOT IMPLEMENTED/PROVEN | Requires Grimholt-owned behavior implementation and tests |
| Vanilla containers/crafting/combat | NOT IMPLEMENTED/PROVEN | Requires Grimholt-owned behavior implementation and tests |
| Datapacks/loot/advancements/scoreboards parity | NOT IMPLEMENTED/PROVEN | Requires targeted snapshot data and behavior coverage |
| 500-1000 player capacity | NOT PROVEN | Benchmark evidence does not exist yet |

## Snapshot baseline

The 26.2 reference currently introduces, among other things:

- Ice Caves biome and associated generation rules.
- Ice Crystals and Icicles.
- Frostbite mob.
- Freezing mob effect.
- Snowball knockback behavior.
- New pathfinding and gameplay tags.
- New block sound-set registry.
- Data Pack version 107.1.
- Resource Pack version 88.0.
- Updated feature, placement, noise and registry data.

These are not optional documentation details: the corresponding server-side data and behavior must be represented in Grimholt before the snapshot can be considered parity-complete.

## Multithreading contract

Minestom already supplies a configurable parallel dispatcher and instance/entity ticking infrastructure. Grimholt will use those facilities without modifying Minestom source.

Grimholt-owned gameplay code must follow these rules:

1. World/chunk/entity mutable state has an explicit owner.
2. A tick owner is the only writer for its owned mutable gameplay state.
3. Cross-owner operations are explicit handoffs, never unsynchronized direct mutation.
4. Heavy I/O, generation and computation are asynchronous only when their results can be applied through the owning tick context.
5. Snapshot/compute/apply is required when computation observes mutable state outside its owner.
6. Plugin APIs must never imply a universal global gameplay thread.
7. Global state is isolated from region-local state.
8. Shutdown cancels or drains owned work deterministically.
9. Bounded queues and backpressure are mandatory for externally generated work.
10. Thread ownership violations are programmer errors and must be observable in development/test mode.

Folia's region ownership documentation is used only as an architectural comparison for these invariants. Grimholt does not depend on or expose Folia APIs.

## Vanilla reference harness

The project must eventually run the same deterministic scenario against:

1. the official vanilla reference server/snapshot where legally and technically practical, and
2. Grimholt.

The harness compares observable behavior such as:

- block state transitions,
- entity state,
- inventories,
- damage/effects,
- scheduled ticks,
- random-tick outcomes under controlled seeds,
- persistence round-trips,
- commands and command tree behavior,
- registry/data synchronization.

A protocol-compatible connection alone is not a parity test.

## Non-negotiable rule

A feature is only marked **Implemented** after executable tests demonstrate the behavior. Minestom delegation is an implementation mechanism, never parity evidence.

## References

- Official Minecraft 26.2: https://feedback.minecraft.net/hc/en-us/articles/49412490179853-Minecraft-Java-Edition-26-4-Snapshot-3
- Minestom: https://github.com/Minestom/Minestom
- Folia region ownership reference: https://docs.papermc.io/folia/reference/region-logic/
