# Chronos: region-parallel simulation kernel

Chronos is Grimholt's planned execution model for scaling one Minecraft world across the CPU resources actually available to the server. It is not a claim that 1,000 players are already supported. It is the design and implementation boundary for proving that claim with reproducible workloads.

## Core thesis

A global tick loop is a poor unit of parallelism. A world is a graph of mutable state with local clusters and a smaller number of cross-boundary interactions. Chronos makes the **region** the unit of ownership and scheduling:

- Each region has one logical writer at a time. Worker threads are interchangeable; ownership belongs to the region, not to a thread.
- Different regions can execute concurrently. The same region never runs two ticks concurrently.
- Cross-region mutation is represented as a bounded, ordered handoff to the target owner. A thread must not directly mutate another region's state.
- The tick clock dispatches work and does not perform gameplay or wait for worker completion.
- Overload is explicit: stale ticks are coalesced, queue saturation is counted, and region duration is measured. No unbounded queue may turn overload into minutes of hidden lag.
- Correctness gates outrank throughput. A skipped tick, delayed cross-region effect, or reordered packet is a correctness/performance trade-off that must be visible and intentional, never accidental.

## Why this is different from “add more threads”

Naively running the whole world tick in parallel creates data races and makes outcomes depend on thread timing. Naively putting every tick in a worker queue creates stale work faster than it can be consumed. Chronos instead separates ownership from execution and makes backpressure part of the runtime contract.

The first implementation, ChronosRegionScheduler, is a bounded dispatch kernel: fixed CPU worker count, a bounded shared queue, one in-flight tick per region, no catch-up bursts, and per-region/global counters. It is an early component, **not yet the production tick engine**. It must not be described as delivering world-wide parallel simulation until it is connected to VanillaRegionManager and verified with integration tests.

## Intended runtime model

### 1. Region-local hot path

A region owns its chunks, block entities, locally owned entities, scheduled tick structures, and local player simulation state. Hot data should migrate toward primitive/structure-of-arrays storage where profiling proves that object graphs and allocation are bottlenecks. Bit-packed indices are appropriate for bounded coordinates, but readability and correctness tests remain mandatory.

### 2. Causal cross-region handoff

A region may emit an intent such as entity transfer, block update across a border, damage, sound, or chunk-interest update. The target region applies it as its own mutation. Handoffs need bounded capacity, ordering, cancellation on shutdown, and explicit failure metrics.

Before shipping, entity migration must be atomic from the simulation's point of view: exactly one owner before and after transfer, no duplicate ticking, and no lost player movement or combat actions. Boundary physics requires a tested halo/ownership protocol rather than “eventually consistent” collision behavior.

### 3. Deadline-aware scheduling

Each region records tick duration, queue delay, missed deadlines, handoff depth, and recent load. A later scheduler stage will use those signals to balance worker assignment and distinguish critical player-facing work from deferrable work. Deferral is only allowed for subsystems whose Minecraft semantics permit it; combat, movement, inventory transactions, and protocol state transitions cannot be silently deprioritized.

The first version deliberately does not dynamically resize worker pools during a tick. Resizing without evidence can cause thread churn and worsen tail latency. Worker count starts from Runtime.availableProcessors() (which modern JVMs generally constrain to container CPU availability); explicit deployment overrides and CPU quota/NUMA inspection belong in the resource-profile stage.

### 4. Logical epochs and determinism

Chronos needs a stable epoch model for ordering cross-region intents and replaying failures. Region-local computation can run concurrently, but externally observable effects must have defined ordering. Tests should replay the same input trace and compare canonical state fingerprints, then compare Grimholt against the pinned vanilla reference for supported mechanics.

Do not add a global barrier just for conceptual simplicity: a barrier makes the slowest region dictate the latency of every region. Conversely, do not remove barriers without defining the causal ordering required by gameplay. The correct boundary is subsystem-specific and must be backed by differential tests.

## Rollout gates

1. Unit tests prove no concurrent ticks for one region, parallel execution for independent regions, bounded queue behavior, shutdown behavior, and error isolation.
2. Integrate the dispatcher with actual region lifecycle; eliminate duplicate scheduling paths rather than layering two clocks over one another.
3. Add deterministic epoch/intent ordering and tests for region-boundary entity transfer, combat, block updates, and shutdown races.
4. Benchmark 1, 100, 500, and 1,000 synthetic players with controlled region distributions. Report p50/p95/p99 tick duration, deadline misses, queue depth, allocation rate, GC pauses, and CPU utilization—not only average operations/second.
5. Run a real Minecraft client and gameplay scenario tests. CI success alone is not proof of client compatibility.
6. Only claim a player capacity after a documented hardware profile, world settings, workload trace, and sustained run meet the target.

## Current status

- Implemented: bounded Chronos dispatch primitive and unit tests for independent-region parallelism, busy-region coalescing, failure isolation, and invalid configuration.
- Not yet implemented: production integration, causal handoff ordering across regions, dynamic load balancing, deterministic replay, comprehensive parity, or a validated 1,000-player benchmark.
- Next engineering step: integrate Chronos with VanillaRegionManager as the single region-tick dispatch path and remove redundant scheduling only after existing ownership tests pass.
