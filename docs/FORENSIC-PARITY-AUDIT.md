# Grimholt forensic Minecraft parity audit

Date: 2026-10-07
Target behavior: Minecraft Java Edition 26.4 Snapshot 3
Repository: HazelTheSquirrel/Grimholt
Branch: main

## Executive finding

The current repository is an engineering foundation, not a complete Minecraft implementation. The existing vanilla package contains deterministic kernel primitives and a few 26.4 winter-mechanic models, but those primitives are not yet connected to Minestom's live world, chunk, entity, player, inventory or network lifecycle.

A real "100% complete" claim requires every gameplay-visible subsystem below to be implemented, wired, and covered by executable tests. Compilation alone is not evidence.

## Status legend

- COMPLETE: implemented, wired into runtime, and verified by executable tests.
- PARTIAL: a primitive exists but is not complete or not runtime-integrated.
- MISSING: no meaningful implementation exists.
- BLOCKED: requires an upstream protocol/runtime capability that Grimholt cannot safely supply while Minestom remains unchanged.

## 1. Runtime and protocol

| Domain | Status | Required for completion |
|---|---|---|
| Handshake/status/login | PARTIAL | End-to-end 26.4 protocol verification |
| Online authentication | PARTIAL | Real client/session verification |
| Encryption/compression | PARTIAL | Protocol-path tests |
| Configuration/play transition | PARTIAL | Full target-version packet/state coverage |
| Registry synchronization | MISSING | Target-version registry bootstrap |
| Packet validation/limits | PARTIAL | Complete packet matrix and fuzz/regression suite |
| Disconnect/timeout semantics | PARTIAL | Full lifecycle coverage |
| 26.4 protocol compatibility | BLOCKED by current Minestom 26.2 substrate | Consume a compatible Minestom release without modifying Minestom |

## 2. World/chunk architecture

| Domain | Status | Required for completion |
|---|---|---|
| Region ownership primitive | PARTIAL | Bind ownership to live chunks/entities |
| Cross-region handoff | PARTIAL | Runtime integration and saturation tests |
| Chunk lifecycle | PARTIAL | Owned load/tick/unload pipeline |
| Chunk persistence | PARTIAL | Complete round-trip and crash tests |
| World time | PARTIAL | Runtime ticking and persistence |
| Weather | MISSING | Rain/thunder/snow/weather rules |
| World border | MISSING | Collision/damage/packet synchronization |
| Simulation distance | MISSING | Correct activation/ticking semantics |
| Dimension rules | MISSING | Overworld/Nether/End behavior |

## 3. Blocks and block entities

Block behavior is currently only represented by a generic immutable BlockState. Completion requires the complete targeted-version block registry and behavior for every block family.

Required families include:
- air/terrain/decorative blocks
- slabs/stairs/walls/fences/gates/doors/trapdoors
- pressure plates/buttons/levers
- crops/plants/vines
- fluids and fluid-containing blocks
- redstone components
- rails
- pistons/observers
- containers
- signs
- beds
- portals
- light/fire
- falling blocks
- snow/ice family including 26.4 Ice Caves content
- every block entity type and its ticking/persistence/network behavior

## 4. Physics and movement

Missing/partial:
- AABB collision
- step-up and step-height rules
- gravity/drag
- jump
- swimming
- climbing
- crawling/sneaking
- sprinting
- flying/creative
- vehicles
- fluids affecting movement
- powder snow
- ladders/vines/scaffolding
- movement validation/anti-cheat semantics
- knockback
- fall damage
- suffocation
- fire/lava damage
- portal travel

## 5. Block update system

Required:
- neighbor updates
- scheduled ticks
- random ticks
- block state transitions
- shape/support checks
- block placement/breaking
- block drops
- tool rules
- harvesting
- block interaction
- fluid scheduling
- fire spread
- crop/growth/decay
- redstone update ordering

The existing scheduled-tick queue is only a kernel primitive.

## 6. Fluids

Missing:
- water source/flow
- lava source/flow
- fluid level propagation
- falling fluid
- water/lava interaction
- fluid tick ordering
- fluid collision and movement effects
- fluid rendering/state synchronization
- fluid persistence

## 7. Redstone

Missing:
- power propagation
- dust shapes/power levels
- torches
- repeaters
- comparators
- levers/buttons/plates
- observers
- pistons/sticky pistons
- target blocks
- dispensers/droppers/hoppers
- note blocks
- update ordering
- quasi-connectivity where applicable
- chunk-boundary behavior
- redstone stress tests

## 8. Items, components and inventories

Missing:
- complete item registry
- ItemStack semantics
- item components/data
- durability
- enchantments
- attributes
- food
- potions
- projectiles
- tools/armor
- pickup/drop
- inventory synchronization
- container menus
- click/drag/shift-click/number-key interactions
- crafting
- recipe book
- item entities
- stack merging
- item despawn

## 9. Crafting and containers

Missing:
- player crafting
- crafting table
- furnace/blast furnace/smoker
- brewing stand
- enchanting table
- anvil
- smithing
- stonecutter
- grindstone
- loom
- cartography
- beacon
- hopper/dispenser/dropper
- shulker boxes
- chests/barrels
- villager trading
- container locking/permissions
- menu packets

## 10. Entities

Missing/partial:
- complete entity registry
- entity lifecycle
- tracking
- collision
- passenger/vehicle relationships
- attributes
- AI goals
- pathfinding
- navigation
- target selection
- breeding
- taming
- despawn
- persistence
- loot
- status effects
- projectile simulation

All Vanilla mob families must be covered, including passive, neutral, hostile, aquatic, flying, utility and boss entities.

## 11. Combat and damage

Missing:
- damage sources/types
- armor/toughness
- enchantment modifiers
- shields
- invulnerability frames
- critical hits
- melee reach/rules
- knockback
- projectiles
- explosions
- fire
- fall
- drowning
- freezing
- suffocation
- starvation
- void
- magic/effects
- death/respawn
- loot/xp

## 12. 26.4 Snapshot 3 additions

Partial only:
- Ice Caves
- Ice Crystals
- Icicles
- Frostbite
- Freezing effect
- Frostbite ice balls
- icicle falling damage
- snow on Packed Ice
- snowball player knockback
- new block sound-set registry
- new pathfinding/gameplay tags
- new worldgen noise/feature/placement data

Existing FreezingState, IceBallImpact and IcicleImpact are models, not yet full live entity/block mechanics.

## 13. Villages, villagers and raids

Missing:
- POI registry
- villager brain
- schedules
- professions
- gossip
- reputation
- trading
- breeding
- restocking
- village detection
- raid trigger
- raid wave composition
- raid spawn positioning
- raid progress
- victory/failure
- Hero of the Village
- patrols

## 14. Dimensions, portals and bosses

Missing:
- Nether rules
- End rules
- portal creation
- portal search/creation
- portal cooldown
- dimension transfer
- respawn anchors
- Ender Dragon
- End crystals
- Wither
- boss bars
- dimension-specific worldgen/rules

## 15. World generation

Missing/partial:
- deterministic seed pipeline
- noise settings
- biome source
- terrain shaping
- aquifers
- carvers
- ores
- vegetation
- structures
- structure sets
- jigsaw structures
- villages
- strongholds
- Nether terrain/structures
- End terrain/islands
- feature placement
- heightmaps
- surface rules
- spawn placement
- 26.4 Ice Cave noise/feature/placement data

Anvil loading existing worlds is not equivalent to Vanilla world generation.

## 16. Data, registries and datapacks

Missing:
- complete registries
- tags
- NBT
- data components
- recipes
- loot tables
- advancements
- predicates
- functions
- schedules
- structures
- worldgen JSON
- damage types
- effects
- enchantments
- instruments/sounds
- datapack loading/reload/error isolation
- resource-pack metadata

## 17. Commands and server state

Partial:
- simple command registration exists

Missing:
- Brigadier-like argument tree
- typed arguments
- suggestions
- command feedback
- permissions
- command source context
- selectors
- execute
- scoreboard
- teams
- bossbar
- gamerules
- difficulty
- gamemode
- teleport
- give/item
- summon
- effect
- data
- function
- reload
- all target-version Vanilla commands

## 18. Player state and gameplay systems

Missing/partial:
- hunger/saturation
- health/absorption
- XP/levels
- effects
- attributes
- abilities
- statistics
- advancements
- recipe unlocks
- spawn/bed/respawn
- death inventory/drop rules
- spectator
- adventure restrictions
- scoreboard/team state
- client settings/capabilities

## 19. Persistence

Partial:
- Anvil loader is wired

Missing:
- complete chunk state round-trip
- entity persistence
- block entities
- player data
- level metadata
- POI data
- raids
- maps
- advancements/statistics
- crash-safe multi-file commit strategy
- migration/version handling
- corruption recovery

## 20. Plugin API

Partial:
- lifecycle, events, commands, scheduler and services exist

Missing for a production-grade API:
- permissions
- region/entity scheduling semantics
- inventory/item/block APIs
- world/chunk APIs
- event ownership/cleanup
- API compatibility/versioning
- classloader isolation hardening
- plugin resource limits
- reload semantics

## 21. Observability/security

Partial:
- bounded scheduler
- security limits
- health/metrics primitives

Missing:
- packet abuse matrix
- per-plugin quotas/metrics
- tick watchdog
- deadlock detection
- memory leak tests
- classloader leak tests
- fuzzing
- long-duration soak tests
- dependency vulnerability automation
- benchmark suite

## 22. Definition of 100% completion

Grimholt must not be declared complete until:
1. Every required subsystem above is implemented and runtime-connected.
2. Every subsystem has focused tests and regression coverage.
3. A real target-version client can connect and exercise ordinary gameplay.
4. World/chunk/entity state is owned by the region model without unsafe cross-thread mutation.
5. Snapshot-specific content is verified against the official reference.
6. Datapack/registry/command behavior is exercised.
7. Persistence survives restart and failure injection.
8. Adversarial concurrency tests pass.
9. CI is green on main.
10. Performance benchmarks exist for the project's stated workloads.
11. Minestom remains unmodified and no Bukkit/Spigot/Paper/Folia runtime dependency exists.

## Current hard blockers

The largest current blocker is protocol/runtime version skew: the repository consumes Minestom 26.2 while the behavioral target is 26.4 Snapshot 3. Grimholt must not fake protocol compatibility or modify Minestom. The correct resolution is to consume a compatible Minestom release when available and keep the Vanilla implementation in Grimholt.

This audit is a living checklist. New official snapshot behavior must be added before it can be considered complete.
