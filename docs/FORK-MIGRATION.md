# Grimholt Fork Migration

## Branch

All fork-engine experiments and structural changes are currently developed on **test**. main remains the stable baseline until a deliberate merge.

## What "fork" means

Grimholt is not intended to become a permanent application layered on top of Minestom.

The migration target is:

```
Minecraft Client
      ↓
Grimholt Protocol / Network
      ↓
Grimholt Runtime Kernel
      ↓
Grimholt Regions / World / Entities / Tick
      ↓
Grimholt API
      ↓
Grimholt Plugins
```

Minestom is the starting codebase and a reference for proven server infrastructure. Relevant source will progressively be imported/adopted into Grimholt, renamed/repackaged where required, modified and owned by Grimholt.

## Ownership rule

Every subsystem must have an explicit answer to:

- Who owns the state?
- Who owns the tick?
- Who owns the lifecycle?
- Which thread/region may mutate it?
- Which public API exposes it?
- Which external dependency is required?

A Minestom type crossing a public Grimholt API boundary is a migration failure.

## Migration order

1. Resource profile — detect CPU and physical memory; establish the 80/20 resource policy.
2. Concurrency kernel — region ownership, bounded queues, handoffs and telemetry.
3. Minestom source inventory — map the runtime into source areas that must become Grimholt-owned.
4. Network ownership — connection, configuration/login/play and packet lifecycle.
5. World ownership — instances, chunks, storage and generation.
6. Entity ownership — players, entities, tracking, physics and AI.
7. Scheduler ownership — authoritative Grimholt ticking and background work.
8. Vanilla parity — behavior/data/protocol correctness for the selected Minecraft baseline.
9. Adaptive runtime — safe rule engine, then optional KI/ML controller.
10. Dependency removal — delete the external net.minestom:minestom runtime dependency.
11. Independent release pipeline — Minecraft releases/snapshots are integrated by Grimholt on its own schedule.

## Reference projects

Other server implementations may be studied for concurrency models, region scheduling, packet handling, memory management, entity activation, chunk I/O, redstone/physics behavior and compatibility edge cases.

They are reference material only. Grimholt does not inherit their APIs, licensing assumptions, runtime dependencies or release cadence.

## Performance guardrails

The 1000-player objective is driven by measurable workload tests.

Required metrics include MSPT, TPS, tick jitter, CPU, heap/GC pressure, queue depth, chunk latency, entity count, network pressure and plugin execution time.

The future adaptive controller must use:

```
Telemetry → Decision → Safety Limits → Controller → Audit/Rollback
```

No AI component may directly mutate arbitrary game state.
