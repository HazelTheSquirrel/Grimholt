# Minecraft version update procedure

Grimholt is pinned to one exact Minecraft release at a time and may advance independently of Minestom. The active baseline is Minecraft Java Edition 26.2.

## Reference inputs

- Reference server JAR: `reference/minecraft/26.2/server.jar`
- Protocol: 776
- World data version: 4903
- Data Pack format: 107.1
- Resource Pack format: 88
- Java: 25
- Reference SHA-1: `823e2250d24b3ddac457a60c92a6a941943fcd6a`

The Mojang server JAR is a build/reference input only. It is never placed on Grimholt's runtime classpath and is never bundled into the standalone Grimholt server.

## Manual update workflow

When moving to a new Minecraft release:

1. Add the exact Mojang `server.jar` under a new immutable path such as `reference/minecraft/<version>/server.jar`.
2. Record and verify its SHA-1.
3. Create/update the corresponding `VanillaSnapshot<version>` contract with the exact protocol, world data, pack formats and Java requirement.
4. Generate reports directly from that exact JAR using the Mojang data generator.
5. Point `VanillaGeneratedData.ROOT` and the Gradle generation task at the new version.
6. Update the active `VanillaSnapshot` facade.
7. Update the Grimholt protocol catalog/codec and all version-specific tests.
8. Inspect Minestom and other server implementations as technical references where useful; do not wait for their releases.
9. Run `generateVanilla<version>`, `test`, the exact vanilla reference smoke test and `assemble`.
10. Only after all checks pass is the new version considered the active Grimholt baseline.

## Independence rule

The reference JAR is never used as Grimholt's gameplay implementation. Its generated reports are authoritative data input for registries/protocol metadata and its executable is used only for differential/reference tests. Vanilla gameplay remains Grimholt-owned.

Minestom source may be imported, adapted and modified as part of the fork migration. A Minestom version bump does not automatically change the Grimholt Minecraft target, and Grimholt must remain able to advance independently.
