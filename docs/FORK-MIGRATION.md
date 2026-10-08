# Grimholt fork migration and completion order

## Branch and target

Implementation is performed on `test`; `main` remains the stable baseline until an intentional merge. Target: Minecraft Java 26.4 Snapshot 3, pinned by SHA-1 `2d89c95c030e635387448f332961074ce1adbb4b`.

## Meaning of independent fork

Grimholt must own protocol, connections, authentication, player/session state, world/chunk/entity simulation, gameplay, data, persistence, scheduler and plugin API. It must run without an external Minecraft server runtime. The Mojang JAR is a test/reference oracle only. No Bukkit/Spigot/Paper/Folia/Velocity API or Minestom runtime dependency is permitted.

## Migration stages

1. Establish reproducible build, dependency and reference gates.
2. Finish connection state machine and prove real-client status/login/configuration/play.
3. Make player, world, chunk and entity models the single authoritative runtime state.
4. Implement core survival loop: movement/physics, block interaction, item drops, inventory, save/reload.
5. Implement block/fluid/redstone tick engine and block entities.
6. Implement item components, menus, recipes, containers, entities, AI and combat.
7. Implement dimensions, portals, villagers, POIs, raids and bosses.
8. Consume generated registries/tags/data and implement datapacks/commands/loot/advancements.
9. Achieve seeded world-generation and lighting parity.
10. Harden concurrency, persistence, security and plugin lifecycle.
11. Run real-client, differential, fuzz, crash-injection, soak and networked scale tests.
12. Publish release artifacts and only then deliberately merge `test` to `main`.

## Ownership checklist for every subsystem

Document its mutable state, sole owner, legal threads, handoff protocol, queue bounds, error behavior, shutdown behavior, persistence format, packet effects, tests and telemetry. Async computation must operate on immutable snapshots and return results to the owner for mutation.

## Reference-project policy

Other server implementations and Minestom may be studied as technical references, subject to license review. Reference reading is not proof of parity. Do not copy code without a compatible license and explicit decision. No external implementation may remain a runtime authority.

## Performance and adaptive control

The 500–1000-player goal is an unproven target, not a promise. Measure realistic networked workloads first. Any future adaptive controller must be deterministic/safety-bounded, auditable and reversible; it cannot mutate arbitrary gameplay state.
