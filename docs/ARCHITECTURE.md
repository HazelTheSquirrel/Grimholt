# Grimholt architecture

## Target architecture

```
Minecraft client
  -> Grimholt network and protocol runtime
  -> session, authentication and packet state machines
  -> region-parallel Chronos simulation kernel
  -> world/chunk/entity state with explicit single-writer ownership
  -> bounded cross-region handoffs and persistence workers
  -> public API and plugins
```

Grimholt is an independent runtime, not an adapter over another Minecraft server implementation. The pinned Mojang server JAR is a reference oracle only. No Bukkit/Spigot/Paper/Folia/Purpur/NMS implementation is part of the runtime.

## Current reality

The current branch contains native Grimholt-owned components for resource profiling, region/world abstractions, player state, protocol framing/packet codecs, command-tree support, authentication primitives, persistence helpers and selected gameplay models. Chronos now exists as a bounded region-dispatch primitive, but is not yet the production tick engine. Its next milestone is integration with the region lifecycle as the single tick-dispatch path.

Chronos is now wired into the production tick lifecycle: registered regions are dispatched automatically through the bounded worker pool, and each tick drains region handoffs under the same exclusive ownership scope. The old global tick path is retained only for compatibility managers and is rejected in Chronos mode. This is scheduling integration, not proof of complete gameplay correctness or 1,000-player capacity.

These components do **not** establish complete runtime ownership or parity. Many protocol packets and codecs are incomplete; real client interoperability is not yet evidenced; world generation, lighting, item/inventory behavior, redstone/fluids, entity AI, dimensions, datapacks and full Anvil/player/block-entity persistence remain open. See FORENSIC-PARITY-AUDIT.md and CHRONOS.md.

## Ownership rules

- Every mutable world, chunk, block-entity, player and entity state has one explicit owner.
- The owner alone mutates its state. Cross-owner changes are messages/handoffs with ordering, failure and backpressure semantics.
- Async work reads immutable snapshots and returns results to the owner before applying mutations.
- Network threads validate and enqueue gameplay intent; they must not mutate arbitrary world state.
- Queues and caches are bounded. Saturation must be visible and have deterministic rejection/backpressure behavior.
- Startup and shutdown are explicit state transitions. All owned executors, sockets, tasks and file handles are closed.
- Global systems (registries, time, weather, scoreboards) must have ownership and synchronization rules distinct from region-local state.
- Plugin APIs document thread/region affinity and cannot expose implementation types.

## Fork integration order

1. Import the pinned upstream source tree and build it from this repository without a published Minestom runtime artifact.
2. Preserve upstream notices and record every local patch; keep upstream package provenance visible.
3. Prove a clean standalone build of the imported upstream baseline before merging Grimholt-specific code.
4. Rebase Grimholt's protocol, gameplay and plugin work onto the fork; remove duplicate competing implementations only after equivalent tests pass.
5. Add CPU-topology-aware worker sizing and per-subsystem metrics; never assume more threads automatically improve throughput.
6. Add deterministic multi-player load tests and compare baseline versus each optimization.

## Runtime layers and completion order

1. Bootstrap/configuration/shutdown.
2. Protocol framing and per-connection state machine.
3. Login, authentication, encryption and compression.
4. Configuration registry/data synchronization.
5. Play packet surface and client synchronization.
6. World/chunk lifecycle, storage and lighting.
7. Player movement/physics and block interactions.
8. Item components, inventory transactions and containers.
9. Scheduled/random ticks, fluids, redstone and block entities.
10. Entities, tracking, AI, damage, combat and projectiles.
11. Dimensions, portals, villages, raids and bosses.
12. Exact world generation, datapacks, commands, recipes, loot and advancements.
13. Hardening, differential tests, real-client tests and networked benchmarks.

Do not jump to adaptive AI or capacity claims while correctness gates are failing.

## Reference isolation

The exact Mojang reference is Minecraft Java 26.4 Snapshot 3, SHA-1 `2d89c95c030e635387448f332961074ce1adbb4b`. Reference generation and smoke tests must run in isolated temporary directories. Reference artifacts must not enter the standalone JAR.
