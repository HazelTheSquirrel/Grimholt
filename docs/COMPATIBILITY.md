# Vanilla compatibility and parity

Grimholt targets the released Minecraft Java Edition 26.2. The official 26.2 release is the behavioral target; Minestom protocol support is not treated as proof of vanilla behavior.

## Current evidence

| Area | Status | Evidence |
|---|---|---|
| 26.2 protocol transport | Implemented | Minestom 26.2 integration |
| Online/offline authentication selection | Implemented in bootstrap | Grimholt config selects online or offline authentication |
| Configurable parallel entity/chunk dispatcher | Implemented | Grimholt configures Minestom dispatcher threads before initialization |
| Anvil world persistence | Wired | 26.2 AnvilLoader with explicit dimension |
| Independent Grimholt plugin API | Implemented foundation | Public API contains no Minestom or SLF4J types |
| Plugin discovery/dependency/lifecycle | Implemented foundation | Descriptor validation, ordering, classloader cleanup |
| Vanilla world generation | NOT IMPLEMENTED/PROVEN | Requires Grimholt-owned vanilla world generation |
| Vanilla physics/fluids/redstone | NOT IMPLEMENTED/PROVEN | Requires Grimholt-owned behavior implementation and tests |
| Vanilla entities/AI/villagers/raids | NOT IMPLEMENTED/PROVEN | Requires Grimholt-owned behavior implementation and tests |
| Vanilla containers/crafting/combat | NOT IMPLEMENTED/PROVEN | Requires Grimholt-owned behavior implementation and tests |
| Datapacks/loot/advancements/scoreboards parity | NOT IMPLEMENTED/PROVEN | Requires targeted 26.2 data and behavior coverage |
| 500-1000 player capacity | NOT PROVEN | Benchmark evidence does not exist yet |

## Non-negotiable rule

A feature is only marked Implemented after executable tests demonstrate it. Minestom delegation is an implementation mechanism, never parity evidence.

## Threading audit references

Minestom already partitions tickable entities across a configurable number of tick threads. Its dispatcher thread setting defaults to one, so Grimholt must configure it explicitly for the multithreaded target.

Paper/Folia region ownership was consulted only as an architectural reference: region-owned state, explicit cross-region scheduling, and no assumption of a single global plugin thread. Grimholt does not depend on Paper/Folia and does not expose their APIs.

## Vanilla reference

Minecraft Java Edition 26.2 is the behavioral source of truth. Mechanics that cannot be established from protocol tests alone require a vanilla/reference-server comparison suite and focused regression tests. Paper may be used temporarily as an implementation-reading aid, never as a runtime dependency or API target.