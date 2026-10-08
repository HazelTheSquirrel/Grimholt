# Grimholt completion work order

> This is the single source of truth for finishing Grimholt as an independent Minecraft Java server implementation. It supersedes stale status statements in earlier notes. A checked box means executable evidence exists on the current implementation line, not that a design or class merely exists.

- **Repository:** `HazelTheSquirrel/Grimholt`
- **Working branch:** `test`
- **Pinned target:** Minecraft Java Edition 26.4 Snapshot 3
- **Reference:** `reference/minecraft/26.4/server.jar`
- **Reference SHA-1:** `2d89c95c030e635387448f332961074ce1adbb4b`
- **Toolchain:** Java 25, Gradle 9.8.0
- **Status at audit:** foundations present; full protocol/client compatibility, gameplay, world generation, persistence parity and scale are not complete.

## 1. Rules of execution

1. Work on `test`; do not silently change `main`.
2. Read this work order, architecture, dependency policy, compatibility matrix and directly related code before each change.
3. Work in dependency order. Do not implement downstream features on unstable ownership or packet contracts.
4. Every logical change must add focused tests and update this document only when evidence justifies a status change.
5. After each change run targeted tests, then `gradle --no-daemon clean test`, `dependencyAudit`, `vanillaReferenceSmoke26_4` and `assemble` as applicable. CI must pass before declaring the task done.
6. No “complete” claim from compilation alone, a reference-server boot, a unit codec test, or a microbenchmark.
7. Use Mojang's exact pinned JAR as a reference/test oracle only; never ship it as Grimholt's implementation.
8. No runtime dependency on Minestom, Bukkit, Spigot, Paper, Folia, Purpur or Velocity.
9. Never claim 500–1000 player capacity without reproducible networked benchmark evidence.
10. All mutable gameplay state has one explicit owner; cross-owner mutation uses bounded handoffs.

## 2. Audit baseline and current evidence

### Confirmed foundations

- [x] Java/Gradle project and Java 25 CI.
- [x] Pinned 26.4 Snapshot 3 reference JAR and checksum verification.
- [x] Mojang reference-server smoke-test task in CI.
- [x] Standalone JAR task and dependency audit.
- [x] Grimholt-owned public API foundations without Minestom types in API signatures.
- [x] Native connection/packet framing and a strict handshake decoder with unit tests.
- [x] Resource budget policy and persistence path/atomic-file primitives with tests.
- [ ] End-to-end Grimholt client compatibility is not yet proven.
- [ ] Full vanilla mechanics/data/worldgen/persistence parity is not implemented.
- [ ] Networked 500–1000-player capacity is not proven.

### Status vocabulary

- **DONE:** wired into the active runtime, tests cover normal and failure paths, and review evidence is recorded.
- **IN PROGRESS:** some implementation exists but at least one required runtime/test gate remains.
- **BLOCKED:** a prerequisite prevents safe implementation; state the exact dependency and unblock condition.
- **NOT STARTED:** no credible implementation/evidence yet.
- **NOT PROVEN:** code may exist, but behavior lacks the required executable evidence.

## 3. Phase A — Baseline, inventory and build integrity

**Goal:** make every later change auditable and reproducible.

- [x] Pin Mojang reference checksum.
- [x] Java 25 / Gradle 9.8 CI baseline.
- [x] Basic tests and standalone artifact task.
- [ ] Run and record all current CI stages on the latest `test` head.
- [ ] Verify clean-checkout build without developer-local files/caches.
- [ ] Audit direct and transitive runtime dependencies and inspect standalone JAR contents.
- [ ] Add source import/package scans for forbidden implementation leakage.
- [ ] Publish a subsystem inventory mapping source packages → runtime entry points → tests → missing behaviors.
- [ ] Ensure docs and version metadata contain no contradictory 26.2-active-target statements.
- **Exit gate:** CI green, artifact launches, checksum/dependency gates pass, inventory reviewed.

## 4. Phase B — Protocol and real-client connectivity

**Goal:** one vanilla 26.4 Snapshot 3 client can connect reliably and enter Play.

### B1. Framing and parser hardening
- [x] Handshake packet ID/protocol/host/port/next-state/trailing-byte validation.
- [ ] VarInt overflow and maximum length validation across every packet.
- [ ] Strict UTF-8 decoding and string-length/code-point limits.
- [ ] NBT depth/size/type/collection limits and malformed-input tests.
- [ ] Unknown packet handling per protocol state; no cross-state packet acceptance.
- [ ] Fuzz tests for truncated, oversized, malformed and adversarial packets.
- [ ] Per-connection packet-rate and byte-rate limits; bounded queues/backpressure.

### B2. Status and login
- [ ] Status response JSON matches target client expectations.
- [ ] Status ping/pong preserves payload and closes cleanly.
- [ ] Login Start, UUID/profile properties and duplicate-session rules.
- [ ] Online-mode session authentication with success/failure/timeout tests.
- [ ] Encryption negotiation, verify token, cipher state transition and encrypted first packet.
- [ ] Compression negotiation and compressed/uncompressed threshold edge cases.
- [ ] Login disconnect reasons and disconnect during every intermediate state.
- [ ] Offline mode behavior and UUID policy documented and tested.

### B3. Configuration
- [ ] Correct target-version packet IDs/codecs for all required configuration packets.
- [ ] Registry data NBT and registry order/identifiers verified against generated reports.
- [ ] Known Packs negotiation and unsupported/missing pack behavior.
- [ ] Feature flags, tags, custom data and resource-pack/cookie packets as applicable.
- [ ] Configuration acknowledgement/finish ordering and duplicate/early packets.
- [ ] Full packet fixtures captured/derived from pinned reference, with legal use boundaries.

### B4. Play bootstrap and session lifecycle
- [ ] Join Game/Play Login payload and player-info bootstrap verified with target client.
- [ ] Initial position/teleport confirmation and movement packet sequence.
- [ ] Keepalive, client settings, plugin/custom payload handling and disconnect.
- [ ] Chunk + light payload renders without client errors.
- [ ] Entity spawn/remove/movement/metadata and player tracking.
- [ ] Reconnect, timeout, server shutdown and malformed-client tests.

**Exit gate:** automated protocol-state tests pass and a real 26.4 Snapshot 3 client repeatedly reaches Play, spawns, moves, sees chunks, disconnects and reconnects. Record exact client build, logs and scenario.

## 5. Phase C — Authoritative world/player kernel

**Goal:** remove split-brain state and make all gameplay mutations ownership-safe.

- [ ] One authoritative player/session model connected to connection lifecycle.
- [ ] World/dimension registry and deterministic world lifecycle.
- [ ] Chunk states: absent → loading → loaded → saving/unloading; cancellation and failure transitions.
- [ ] Section/block-state storage, block entities, heightmaps, light and dirty tracking.
- [ ] Region/owner mapping for chunks, entities and block entities.
- [ ] Bounded cross-owner command queue with ordering, rejection, timeout and metrics.
- [ ] Entity ownership transfer and region/chunk unload race handling.
- [ ] Network thread only validates and enqueues gameplay intent.
- [ ] Startup/shutdown and partial initialization cleanup.
- [ ] Tests for join/quit during chunk load, simultaneous unload, region handoff, save and shutdown.
- **Exit gate:** no duplicated authoritative player/world state; race/stress tests pass with ownership assertions enabled.

## 6. Phase D — Minimum playable survival loop

Implement in this order because each step depends on the previous state model.

1. **Movement and collision:** AABB, ground detection, gravity/drag, jumping, sprint/sneak, swim/climb, flight/creative, step-up, knockback, movement correction, fall/suffocation/fire/lava/drowning damage.
2. **Block interaction:** reach and permissions, ray/face validation, break progress, tool/harvest rules, placement/support/replaceability, neighbor updates, drops and block-state synchronization.
3. **Item foundation:** item registry, stack limits, item components, stack equality/merge, durability, pickup/drop and item-entity lifecycle.
4. **Inventory protocol:** authoritative slots, cursor, click/drag/shift-click/number-key/drop actions, transaction validation, container open/close and resync after invalid actions.
5. **Persistence baseline:** save/load player position, game mode, health/hunger, inventory and changed chunks; restart tests preserve state.
6. **Client polish:** health, food, XP, abilities, effects, respawn/death and dimension/teleport state as implemented.

**Exit gate:** a real client can join a generated world, move, break/place blocks, collect/drop items, use a basic inventory, disconnect, restart the server and recover the expected world/player state.

## 7. Phase E — Blocks, ticks, fluids and redstone

- [ ] Complete target block/state registry and stable identifier mapping.
- [ ] Shape/support/collision/interaction contracts for each block family.
- [ ] Scheduled ticks with deterministic order, cancellation and persistence policy.
- [ ] Random ticks seeded and owner-local.
- [ ] Neighbor updates and update suppression/recursion protection.
- [ ] Water/lava sources, flow levels, falling fluids, mixing, fluid collision and scheduled flow.
- [ ] Fire spread/extinguish and crop/plant growth/decay.
- [ ] Block entity registration, ticking, packets and persistence.
- [ ] Redstone power propagation, dust, torches, repeaters, comparators, levers/buttons/plates, observers, pistons, sticky pistons, dispensers/droppers/hoppers and note blocks.
- [ ] Quasi-connectivity, update ordering and chunk-border behavior where vanilla uses them.
- [ ] Adversarial clocks, piston loops, fluid floods and huge farms.
- **Exit gate:** each block family has focused tests; seeded deterministic scenarios are compared with the vanilla reference.

## 8. Phase F — Items, menus, recipes and data-driven gameplay

- [ ] Full item/component model for the target version.
- [ ] All menu types and exact serverbound click semantics.
- [ ] Crafting, recipe book, furnace/blast furnace/smoker, brewing, enchanting, anvil, smithing, stonecutter, grindstone, loom, cartography, beacon, hopper, dispenser/dropper and shulker/chest/barrel behavior.
- [ ] Recipe parsing, unlocks, cooking time/fuel and container item rules.
- [ ] Loot tables, loot conditions/functions, drops and XP.
- [ ] Enchantments, attributes, durability, food, potions, effects and equipment.
- [ ] Tags, registries, predicates, functions, schedules and datapack loading/reload/error isolation.
- [ ] Advancements, statistics, scoreboards, teams, boss bars and gamerules.
- **Exit gate:** deterministic recipes/loot/datapack fixtures and real-client container scenarios match the pinned reference.

## 9. Phase G — Entities, AI, damage and combat

- [ ] Complete entity registry, type data, spawn/remove/tracking and metadata.
- [ ] Entity attributes, collision, movement and passenger/vehicle relationships.
- [ ] Damage sources/types, armor/toughness, enchantment modifiers, invulnerability frames and knockback.
- [ ] Melee, shields, critical hits, projectiles, arrows, tridents and snapshot-specific projectiles.
- [ ] Status effects, death, drops, XP, despawn and entity persistence.
- [ ] Mob goals, pathfinding, target selection, breeding, taming, leash and loot behavior.
- [ ] Passive, neutral, hostile, aquatic, flying, utility and boss entity families.
- [ ] Villagers, POIs, schedules, professions, gossip, trading/restock, village detection and raids/waves/rewards.
- [ ] Bosses, boss bars, Ender Dragon, Wither and related mechanics.
- **Exit gate:** entity family matrix with behavior tests, client visibility and persistence tests.

## 10. Phase H — Dimensions and world generation

- [ ] Overworld, Nether and End rules and dimension transfer.
- [ ] Portals: creation/search, destination, cooldown and failure behavior.
- [ ] Deterministic seed pipeline and versioned noise settings.
- [ ] Noise router/density functions, biome source, aquifers and carvers.
- [ ] Surface rules, ores, vegetation, placed features, placement modifiers and structure sets.
- [ ] Jigsaw structures, villages, strongholds, monuments and dimension-specific structures.
- [ ] Nether terrain/structures and End islands/structures.
- [ ] Spawn placement, heightmaps and non-empty chunk lighting/propagation.
- [ ] Snapshot-specific Ice Caves, Ice Crystals, Icicles, Frostbite/freezing and related tags/features confirmed from generated reports.
- [ ] Seeded golden tests compare block samples, biome columns, heightmaps and structure locations with the pinned reference.
- **Exit gate:** deterministic worldgen fixtures across many seeds and chunk boundaries; saved chunks reopen correctly and render with correct lighting.

## 11. Phase I — Commands, server rules and administration

- [ ] Complete command dispatcher/argument grammar and suggestions.
- [ ] Command source context, permission checks, feedback and error semantics.
- [ ] Core commands: help, list, stop, save, gamemode, difficulty, gamerule, teleport, give/item, summon, effect, time/weather, world border, kill, experience and related target commands.
- [ ] Selectors, execute, data, function, reload and scoreboard/team/bossbar semantics.
- [ ] Command tree packet matches runtime-registered commands.
- [ ] Permission/command tests from console and player contexts.
- **Exit gate:** each implemented command has parser, permission, output and failure tests; client command tree agrees with server dispatcher.

## 12. Phase J — Persistence and compatibility

- [ ] Exact Anvil region/chunk read/write, section palettes, block entities, light, heightmaps and ticks.
- [ ] Player data, entities, POI, raids, maps, statistics, advancements and level metadata.
- [ ] Data-version conversion and migration policy.
- [ ] Crash-safe write ordering, temporary files, atomic replacement and multi-file consistency.
- [ ] Corruption detection, recovery/quarantine and clear diagnostics.
- [ ] Failure injection: disk full, permission denied, process interruption and truncated NBT.
- [ ] Round-trip tests for every persistent subsystem and compatibility fixtures from the pinned reference.
- **Exit gate:** restart and failure-injection suite passes without silent data loss or unsafe path traversal.

## 13. Phase K — Security, concurrency and observability

- [ ] Packet length/rate/CPU abuse limits and malformed input fuzzing.
- [ ] Bounded queues and explicit backpressure for network, region, generation and storage work.
- [ ] Shutdown drains/cancels all owned work with timeouts.
- [ ] Tick watchdog, deadlock/thread-leak detection and structured metrics.
- [ ] Metrics: TPS/MSPT/jitter, CPU, heap/GC, players, entities, chunks, queue depth/wait, chunk I/O, network bytes and plugin execution time.
- [ ] Plugin listener/task cleanup, class-loader leak test and API compatibility policy.
- [ ] No hidden global locks or cross-owner direct writes.
- [ ] Long soak, randomized join/quit, chunk churn and queue-saturation tests.
- **Exit gate:** adversarial test suite passes; no unbounded growth or orphaned threads/resources.

## 14. Phase L — Performance, release and independent updates

- [ ] Build a networked client/load harness, not just a logical world-model microbenchmark.
- [ ] Measure 50/100/250/500/750/1000 players under documented CPU, RAM, JVM, world, view/simulation distance, entity density and plugin workload.
- [ ] Report TPS, p50/p95/p99 MSPT, GC pauses, memory, network throughput, queue latency and disconnects.
- [ ] Define supported platform/resource envelope and publish results; no universal capacity guarantee.
- [ ] Reproducible standalone artifact and dependency/license report.
- [ ] Fresh-install, upgrade, backup/restore and rollback documentation.
- [ ] Snapshot update procedure validates JAR checksum, regenerates reports, updates protocol/data contracts and reruns differential tests.
- [ ] Only after all mandatory gates pass: tag a release and deliberately merge `test` into `main`.

## 15. Release definition of done

A release is blocked until all of the following are true:

- [ ] Target-version client status/login/configuration/play verified.
- [ ] Basic survival loop and core gameplay systems work end to end.
- [ ] Protocol/registry/data/command coverage is version-accurate.
- [ ] Seeded world generation and lighting have differential evidence.
- [ ] All persistent state survives restart and failure injection.
- [ ] Concurrency/fuzz/soak suites pass.
- [ ] Clean CI is green on the exact release commit.
- [ ] Standalone artifact contains no forbidden server runtime dependency and does not bundle the reference JAR.
- [ ] Performance claims match measured workload evidence.
- [ ] Docs, limitations and upgrade path match shipped behavior.

## 16. Execution protocol for every pull/commit

Before editing: check branch/head, relevant source and tests, architecture, compatibility matrix, dependency policy and current CI.

After editing:
1. inspect the full diff;
2. run focused tests;
3. run clean test and dependency audit;
4. run reference smoke and assemble where applicable;
5. inspect CI result/logs;
6. perform an adversarial pass;
7. update status only with evidence.

**Do not mark a phase complete to make the roadmap look finished. The purpose of this work order is to drive implementation to verified completion.**
