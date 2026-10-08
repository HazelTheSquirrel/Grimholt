# Minecraft version update procedure

## Active baseline

Minecraft Java Edition 26.4 Snapshot 3. Reference JAR `reference/minecraft/26.4/server.jar`, SHA-1 `2d89c95c030e635387448f332961074ce1adbb4b`. Metadata currently records protocol `1073742165`, world data version `5122`, Data Pack `123.0`, Resource Pack `100.0`, Java 25.

## Update workflow

1. Choose an exact release/snapshot and record its official version identity.
2. Download the official JAR into a new immutable `reference/minecraft/<version>/server.jar` path; do not overwrite the old baseline.
3. Verify and record the official checksum. CI must fail on mismatch.
4. Run the official data generator from that exact JAR in an isolated temporary directory.
5. Review generated registry, tag, command, component, recipe, loot, biome, noise, feature, placement, structure and damage/effect reports.
6. Update version contract/manifest and active-version facade together; remove contradictory version constants.
7. Update packet IDs/codecs and fixtures for every protocol state.
8. Add/update seeded worldgen and behavior differential tests.
9. Update this document, README, compatibility matrix and work order.
10. Run clean tests, dependency audit, reference smoke, generated-data validation, real-client tests and assemble.
11. Promote the new baseline only when all mandatory gates pass. A reference-JAR update alone is not a compatible release.

## Independence and legal boundary

The reference JAR is a data/test oracle only, not Grimholt's runtime. Never package it in the standalone artifact. Grimholt owns its release cadence independently of other server projects. Study other projects as references only, and review license obligations before adopting source.
