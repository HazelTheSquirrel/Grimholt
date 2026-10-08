# Vanilla compatibility and parity

Grimholt uses two deliberately separate version anchors:

- **Vanilla behavior reference:** Mojang Minecraft Java Edition **26.4-snapshot-3** (pinned reference JAR SHA-1 `2d89c95c030e635387448f332961074ce1adbb4b`).
- **Current Minestom runtime/protocol foundation:** No external Minestom runtime dependency; Grimholt-owned protocol/runtime migration is in progress.

The snapshot is the source of truth for gameplay semantics, world rules, registries, data-driven behavior and concurrency-safe ownership requirements. Minestom is the source foundation being forked, not the authority for Grimholt compatibility.

## Version policy

| Layer | Version | Rule |
|---|---|---|
| Vanilla behavior | 26.4-snapshot-3 | Mandatory behavioral reference |
| Vanilla data | 26.2 | Target data/registry semantics |
| Grimholt runtime | Independent | Must ultimately be fully Grimholt-owned |
| Grimholt API | Independent | Must not expose Minestom implementation types |
| Multithreading | Grimholt-owned | Runtime ownership and scheduling are Grimholt responsibilities |

Grimholt may advance to a newer Minecraft release or snapshot independently of Minestom. Minestom releases are technical references and source inputs, not release gates.

## Current evidence

| Area | Status | Evidence |
|---|---|---|
| 26.4-snapshot-3 protocol transport | PARTIAL | Grimholt-owned framing and packet catalog; client interoperability is not yet proven |
| Online/offline authentication | PARTIAL | Login flow exists; full real-client compatibility and edge-case validation remain open |
| Configurable parallel dispatcher | Implemented | Grimholt configures Minestom dispatcher threads before initialization |
| World persistence | PARTIAL/NOT PROVEN | Atomic file store exists; complete Anvil/chunk/player parity and restart tests remain open |
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

The pinned 26.4-snapshot-3 reference is the sole target for generated registries, protocol reports and behavioral differential tests.

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

Grimholt is building its own scheduling and ownership model. Existing Minestom facilities may be used temporarily behind migration boundaries, but they are not the final runtime authority.

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
- Minestom source is used as a fork/reference source.
- Other Minecraft server implementations are architectural/reference material only.
