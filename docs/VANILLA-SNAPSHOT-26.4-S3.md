# Grimholt — Vanilla 26.4 Snapshot 3 Reference

**Reference snapshot:** Minecraft Java Edition 26.4 Snapshot 3  
**Published:** 2026-10-06  
**Role:** behavioral and data reference for Grimholt

This document is a living engineering reference. It is not a release checklist and must not be copied into historical review files.

## Required parity domains

### World and generation
- Ice Caves biome.
- Ice Cave terrain and cave generation rules.
- Icicle and Ice Crystal generation.
- Snapshot feature/placement/noise changes.
- Ore replacement rules and relevant tags.

### Blocks
- Ice Crystal support/break behavior.
- Icicle support, growth and placement behavior.
- Snow placement on Packed Ice.
- Block sound-set registry and block sound behavior.
- Any changed block tags and neighbor/update behavior.

### Entities and effects
- Frostbite entity.
- Frostbite attacks and freezing interaction.
- Freezing mob effect.
- Frostbite spawning rules.
- Frostbite preferred-weapon behavior.
- Snowball player knockback.

### Data and registries
- Data Pack version: 123.0.
- Resource Pack version: 100.0.
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
- **behavioral target:** this 26.4 Snapshot 3 reference.

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
