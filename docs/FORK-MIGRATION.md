# Grimholt fork migration and completion order

## Branch and target

Implementation is performed on `test`; `main` remains the stable baseline until an intentional merge. Target: Minecraft Java 26.4 Snapshot 3, pinned by SHA-1 `2d89c95c030e635387448f332961074ce1adbb4b`.

## Meaning of independent fork

Grimholt is a **source fork of Minestom**, not a wrapper around the published `net.minestom:minestom` artifact and not a ground-up rewrite of every networking/world subsystem. Minestom implementation source must be checked into this repository, compiled here and directly modifiable by Grimholt. The runtime dependency on the published Minestom artifact remains forbidden. The Mojang JAR is a test/reference oracle only. No Bukkit/Spigot/Paper/Folia/Purpur/Velocity API is permitted.

The repository is not yet at that end state: the current `test` branch is the pre-fork transition baseline. The pinned upstream and licensing requirements are recorded in `UPSTREAM.md`.

## Migration stages

1. Establish reproducible build, dependency and reference gates (current baseline).
2. Import the pinned Minestom source tree into Grimholt and build it as first-party source; retain upstream license/NOTICE and mark modified upstream files.
3. Produce a clean standalone artifact from the imported source and verify its tests before porting local changes.
4. Map Grimholt's existing network, connection, world and scheduler code against upstream equivalents; choose one authoritative implementation per subsystem and port valuable changes instead of maintaining duplicate stacks.
5. Prove real-client status/login/configuration/play on the source-fork baseline.
6. Make player, world, chunk and entity models the single authoritative runtime state.
7. Implement core survival loop: movement/physics, block interaction, item drops, inventory, save/reload.
8. Implement block/fluid/redstone tick engine and block entities.
9. Implement item components, menus, recipes, containers, entities, AI and combat.
10. Implement dimensions, portals, villagers, POIs, raids and bosses.
11. Consume generated registries/tags/data and implement datapacks/commands/loot/advancements.
12. Achieve seeded world-generation and lighting parity.
13. Harden concurrency, persistence, security and plugin lifecycle.
14. Run real-client, differential, fuzz, crash-injection, soak and networked scale tests.
15. Publish release artifacts and only then deliberately merge `test` to `main`.

## Ownership checklist for every subsystem

Document its mutable state, sole owner, legal threads, handoff protocol, queue bounds, error behavior, shutdown behavior, persistence format, packet effects, tests and telemetry. Async computation must operate on immutable snapshots and return results to the owner for mutation.

## Reference-project policy

Minestom is the selected source upstream under Apache License 2.0. Preserve its license, copyright and NOTICE material; mark modified upstream files and track the pinned upstream commit. Other server implementations may be studied as technical references subject to their licenses, but must not be copied without a compatible license review. No external server artifact may remain a runtime authority.

## Performance and adaptive control

The 500–1000-player goal is an unproven target, not a promise. Measure realistic networked workloads first. Any future adaptive controller must be deterministic/safety-bounded, auditable and reversible; it cannot mutate arbitrary gameplay state.
