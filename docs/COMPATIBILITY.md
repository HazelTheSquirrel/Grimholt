# Minecraft compatibility and parity evidence

## Pinned target

- **Minecraft Java Edition:** 26.4 Snapshot 3
- **Reference JAR:** `reference/minecraft/26.4/server.jar`
- **SHA-1:** `2d89c95c030e635387448f332961074ce1adbb4b`
- **Toolchain:** Java 25 / Gradle 9.8.0

This is the target for protocol, registries, generated data, gameplay behavior and differential tests. The reference JAR is not a runtime implementation.

## Evidence matrix

| Area | Current status | Evidence / remaining gate |
|---|---|---|
| Reference pin and checksum | Implemented | CI checks the checked-in JAR SHA-1 |
| Mojang reference boot | CI smoke test | Proves the reference starts; not Grimholt compatibility |
| Grimholt build / tests | CI gate | Latest run must be checked per commit |
| Grimholt-owned handshake parsing | Partial | Strict field/trailing-byte validation and unit tests; not a full protocol implementation |
| Status/login/configuration/play | Partial / unverified | Full packet matrix, real target client and disconnect/fuzz tests required |
| Online authentication/encryption | Partial / unverified | Primitive tests exist; real session/client round trip and failure-path tests required |
| Registry/configuration sync | Partial | Complete NBT/registry/custom datapack behavior required |
| Player movement and synchronization | Partial | End-to-end movement, teleport, abilities, health, inventory and reconnect tests required |
| World/chunk transport and lighting | Partial | Non-empty chunk, light propagation, border and client-render verification required |
| Block/item/entity mechanics | Incomplete | Runtime-connected vanilla behavior and differential tests required |
| World generation | Incomplete | Seeded terrain/biome/structure comparisons required |
| Persistence | Partial / unverified | Complete chunk/player/entity/block-entity round-trip, restart and crash-injection required |
| Plugin API | Foundation | Compatibility policy and gameplay APIs remain open |
| Multithreading safety | Partial | Ownership stress, queue saturation and shutdown race tests required |
| 500–1000 concurrent players | Not proven | Networked load tests and long soak evidence required |

## Snapshot data baseline

The generated reference metadata currently records protocol `1073742165`, world data version `5122`, data-pack version `123.0`, resource-pack version `100.0`, and Java major `25`. Verify these against the pinned JAR and generated reports whenever the reference changes; never copy these values into another version's metadata without regeneration.

The snapshot-specific content list is derived from the exact pinned reference and must be confirmed against generated registries/tags, not inferred from names alone. Ensure Ice Caves, Ice Crystals, Icicles, Frostbite/freezing behavior, snowball knockback and related worldgen/tag/sound changes are represented where present in the actual generated reports.

## Acceptance policy

A feature is only marked implemented when it is connected to the active runtime and executable tests demonstrate its behavior. A packet codec test is not a client interoperability test; loading an Anvil file is not world generation; a world-model microbenchmark is not a server-capacity benchmark.

The release gate requires:
1. supported target client connects and enters Play;
2. ordinary movement, block interaction, inventory and reconnect work;
3. generated world and saved state survive restart;
4. reference-differential and adversarial tests pass;
5. the standalone artifact runs without external server implementation dependencies;
6. all remaining limitations are explicit.
