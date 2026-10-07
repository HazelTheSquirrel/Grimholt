# Grimholt — Vanilla 26.2 Reference

**Reference snapshot:** Minecraft Java Edition 26.2  
**Published:** 2026-06-16  
**Role:** behavioral and data reference for Grimholt

This document is a living engineering reference. It is not a release checklist and must not be copied into historical review files.

## Required parity domains

### World and generation
- Sulfur Caves biome and the 26.2 world-generation changes.
- 26.2 terrain, cave and sulfur-generation rules.
- Sulfur, cinnabar and sulfur-cube related generation.
- Snapshot feature/placement/noise changes.
- Ore replacement rules and relevant tags.

### Blocks
- 26.2 block support, break and interaction behavior.
- Sulfur/cinnabar block behavior and relevant placement rules.
- Snow and terrain placement rules introduced by 26.2.
- Block sound-set registry and block sound behavior.
- Any changed block tags and neighbor/update behavior.

### Entities and effects
- Sulfur Cube entities and their 26.2 behavior.
- Sulfur Cube absorption and interaction behavior.
- 26.2 status/effect changes.
- Sulfur Cube spawning rules.
- 26.2 entity AI and interaction changes.
- 26.2 projectile/entity interaction changes.

### Data and registries
- Data Pack version: 107.1.
- Resource Pack version: 88.0.
- New block sound-set registry.
- New tags.
- New particle/entity/item/block model and sound references required by server-visible data.
- Updated pathfinding tags.

## Concurrency implications

The snapshot is a behavioral reference, not a threading prescription.

Grimholt must preserve vanilla-observable ordering while changing execution topology. In particular:

- A block update must be applied by the owner of the affected world state.
- Fluid, redstone and scheduled tick work must not directly mutate another owner’s state.
- Entity behavior must execute with exclusive ownership of the entity's mutable state.
- Cross-owner effects must be queued as explicit handoffs.
- Asynchronous calculations must use immutable/snapshotted inputs and return results to the owner before mutation.
- Random tick behavior must remain deterministic for a given world state and random source.
- Merge/split/ownership changes must not expose partially migrated state to gameplay code.

## Version boundary

Minestom currently exposes the 26.2 runtime/protocol foundation used by Grimholt. Grimholt does **not** modify Minestom source to obtain 26.4 behavior.

Until Minestom supports the newer protocol/runtime, Grimholt must keep:

- **runtime compatibility:** Minestom-supported version;
- **behavioral target:** this 26.2 reference.

The project must not claim 26.4 client compatibility solely because 26.4 behavior is being implemented internally.

## Verification rule

Every implemented snapshot feature requires:

1. an executable Grimholt test where deterministic;
2. a regression test for discovered edge cases;
3. a reference comparison where the behavior cannot be specified completely from public protocol/data documentation;
4. a concurrency/ownership review;
5. a performance review if the feature is tick-hot.

## Source

Official Mojang/Minecraft Feedback snapshot notes:
https://feedback.minecraft.net/hc/en-us/articles/49412490179853-Minecraft-Java-Edition-26-4-Snapshot-3
