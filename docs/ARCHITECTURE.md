# Grimholt architecture

## Target architecture

```
Minecraft client
  -> Minestom-derived networking / protocol / connection lifecycle (fork-owned source)
  -> Grimholt session, gameplay and public API extensions
  -> Minestom-derived world/chunk/entity primitives (fork-owned source)
  -> Grimholt-owned region/tick scheduling and cross-owner handoff kernel
  -> Plugins
```

The target is a source-level Minestom fork. Minestom source is compiled inside Grimholt and becomes modifiable Grimholt-owned code; `net.minestom:minestom` must remain absent from the runtime dependency graph. The pinned Mojang server JAR is a reference oracle only. No Bukkit/Spigot/Paper/Folia/Purpur API is allowed. See `UPSTREAM.md` for the upstream commit, license obligations and update policy.

## Current reality

The current branch is a transition baseline, not yet the completed source fork: it still contains native Grimholt-owned components for resource profiling, region/world abstractions, player state, protocol framing/packet codecs, command-tree support, authentication primitives, persistence helpers and selected gameplay models. The next architectural milestone is importing the pinned Minestom source tree into this repository and reconciling overlapping subsystems. The 26.4 handshake parser now validates packet ID, protocol, host, port, next state and trailing bytes.

These components do **not** establish complete runtime ownership or parity. Many protocol packets and codecs are incomplete; real client interoperability is not yet evidenced; world generation, lighting, item/inventory behavior, redstone/fluids, entity AI, dimensions, datapacks and full Anvil/player/block-entity persistence remain open. See `FORENSIC-PARITY-AUDIT.md`.

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
