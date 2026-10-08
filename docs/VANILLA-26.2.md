# Grimholt — Minecraft 26.4 Snapshot 3 target checklist

> The filename is retained for repository history compatibility. The active target is **26.4 Snapshot 3**, not 26.2. Do not use the older 26.2 reference as the active behavior or protocol baseline.

## Pinned reference

- JAR: `reference/minecraft/26.4/server.jar`
- SHA-1: `2d89c95c030e635387448f332961074ce1adbb4b`
- Protocol metadata: `1073742165`
- World data version: `5122`
- Data Pack version: `123.0`
- Resource Pack version: `100.0`
- Java major: `25`

Treat build-generated metadata and reports from the pinned JAR as authoritative; re-verify every value if the reference changes.

## Required target-specific coverage

- Complete registries and tags, including block sound sets and pathfinding/gameplay tags.
- Ice Caves biome and related noise, placed features, placement modifiers and surface rules where present in generated reference data.
- Ice Crystals and Icicles block/state/shape/interaction/ticking/persistence behavior.
- Frostbite entity, freezing effect, projectile behavior and damage/knockback interactions.
- Snow placement on Packed Ice and other target-specific block updates.
- All protocol, component, command, pack-format and registry changes visible in generated reports.
- Worldgen/seed fixtures and client synchronization for all target-specific content.

## Required proof per feature

1. Confirm the feature exists in generated reports from the pinned JAR.
2. Implement data and behavior in Grimholt-owned runtime code.
3. Add deterministic focused and regression tests.
4. Compare observable output with the official reference where practical.
5. Verify packet/data synchronization with a real client.
6. Review tick ownership, persistence, failure handling and performance.

The existence of a model named after a feature does not mean the feature is playable or complete.
