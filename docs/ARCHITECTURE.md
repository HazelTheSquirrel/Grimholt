# Grimholt Architecture

## Core principle

Grimholt is a **Minecraft server fork**, not a Bukkit compatibility layer, Minestom plugin, or permanent wrapper around an external Minestom runtime.

The public dependency direction is:

Minecraft client -> Grimholt server runtime -> Grimholt API -> Grimholt plugins

Minestom is the source foundation from which Grimholt is being forked. During the migration, external Minestom code may remain behind internal boundaries, but those boundaries are temporary.

## Strategic end state

The final architecture is:

Minecraft client -> Grimholt protocol/runtime -> Grimholt region/world/entity simulation

Grimholt must own:

- protocol and connection lifecycle,
- configuration/login/play handling,
- world and chunk state,
- entity and player state,
- gameplay ticking,
- physics and interactions,
- vanilla block/item/entity behavior,
- persistence,
- scheduling and region ownership,
- plugin lifecycle and public API.

An external Minestom Maven artifact must **not** be required at runtime in the final architecture.

## Fork migration

The project is intentionally migrating in stages:

1. establish Grimholt-owned kernel and public API;
2. import/adopt the required Minestom source foundation into Grimholt;
3. establish Grimholt package and ownership boundaries;
4. move network/protocol ownership into Grimholt;
5. move instance/chunk/world ownership into Grimholt;
6. move entity/player simulation into Grimholt;
7. make Grimholt scheduling/ticking authoritative;
8. complete Grimholt-owned Minecraft parity;
9. remove the external `net.minestom:minestom` dependency;
10. maintain future updates as deliberate Grimholt fork integrations.

## Current transitional boundary (2026-10-07)

Today, Minestom still provides substantial low-level runtime functionality, including the live transport/connection path and instance substrate. Grimholt owns the higher-level kernel, API, world model abstractions and growing vanilla implementation.

This is **not the final architecture**.

The distinction is important:

- **Current:** Grimholt is built on Minestom.
- **Target:** Grimholt is a fork derived from Minestom, with Grimholt owning the resulting server runtime.
- **Final:** No external Minestom server runtime is needed to run Grimholt.

Minestom therefore remains an important upstream/source foundation, but it is not allowed to become the permanent authoritative implementation of Grimholt gameplay or server behavior.

## Concurrency model

The design must avoid a single giant global lock.

State ownership must be explicit. Where state can be partitioned by world, chunk, player or subsystem, ownership should be partitioned as well.

CPU-heavy work such as world generation, storage I/O and expensive calculations must be separated from latency-sensitive region execution and must have bounded queues/backpressure. World/chunk/entity state is owned by its current tick partition; cross-partition work must be explicitly handed off.

The API must not force plugins to assume that every operation executes on one global server thread. Location-bound work belongs to the owner of that location; entity-bound work follows the entity; global work uses a global coordinator; blocking I/O is asynchronous.

At startup Grimholt detects logical processor capacity and host memory. The runtime uses an 80% server resource budget for physical memory and reserves 20% for the operating system and safety margin. CPU capacity is used as scheduling input; actual scaling is validated with measurements rather than assumed.

## Plugin safety

A plugin must never receive unrestricted references to internal implementation objects.

The API should expose capabilities, not implementation classes.

## Performance rules

Do not optimize from intuition.

Every major subsystem should eventually have:
- a functional test,
- a regression benchmark where appropriate,
- allocation awareness,
- contention awareness,
- a documented ownership model.

The 500-1000 player target is treated as a workload requirement that drives architecture, not as a marketing number.

## Dependency policy

Forbidden runtime dependencies:
- Bukkit
- Spigot
- Paper
- Folia

Minestom is currently an allowed **transitional source/runtime dependency**, but it is not an allowed permanent final runtime dependency.

Every new dependency must have:
- a concrete reason,
- license compatibility,
- maintenance assessment,
- performance impact assessment,
- security impact assessment.

## Compatibility policy

The targeted Minecraft version is the source of truth for protocol and vanilla behavior.

Do not silently implement behavior from another Minecraft version merely because it is convenient.

## Definition of done

A feature is done only when:
1. implementation exists,
2. tests exist where meaningful,
3. failure modes were reviewed,
4. dependency boundaries were checked,
5. concurrency implications were checked,
6. the feature was re-reviewed after implementation.