# Grimholt independent core — foundation

## Goal

Grimholt's product core is an independent implementation. Minestom is a temporary behavioral/reference environment only; it must not be imported by core packages or used as a source of copied implementation code. A later migration may keep a separate compatibility harness, but shipping core modules must build and test without Minestom.

## Architecture boundaries

- `transport`: socket lifecycle, framing, compression/encryption adapters and backpressure.
- `protocol`: version-specific packet codecs and protocol state transitions; no world rules.
- `session`: login, identity, connection lifecycle and ordered outbound delivery.
- `world`: block/chunk data, coordinate math, chunk lifecycle and persistence contracts.
- `simulation`: authoritative gameplay rules, inventory, damage, hunger and entity interactions.
- `runtime`: scheduling, region ownership, bounded queues, lifecycle and hardware-aware capacity planning.
- `storage`: asynchronous persistence and recovery; never block a simulation owner.
- `observability`: allocation, queue-depth, tick-latency and per-region metrics.

These are logical boundaries first, not a demand to create a large Maven multi-module build before contracts stabilize.

## Concurrency model

1. Network workers perform bounded IO and decode enough protocol state to create validated commands.
2. Commands are handed to the owning session/region through bounded queues. Saturation has an explicit policy; expensive work never falls back to the caller thread.
3. Each mutable world region has one simulation owner at a time. Cross-region work is an explicit message, not shared mutable state protected by a global lock.
4. Chunk generation and persistence run asynchronously. Results return as messages and are applied by the owner after validation.
5. Outbound packets are built from a consistent snapshot and handed to transport without blocking simulation.
6. Shutdown stops admission, drains or rejects queued work according to policy, persists required state, then closes workers.

A global tick loop or the phrase “zero allocation” is not an architecture by itself. We will measure allocation rate, queue wait, p50/p95/p99 tick duration, network throughput and memory under repeatable loads. Pooling and custom layouts are introduced only when profiling demonstrates a win.

## Implementation order

1. **Pure core primitives** — coordinate keys, block/state identifiers, bounded queues and deterministic tests. No Minestom dependency.
2. **Protocol/transport spike** — handshake, status, login and play-state framing for one explicitly pinned Java Edition protocol.
3. **Session lifecycle** — connect, authenticate as supported, join, disconnect and clean shutdown.
4. **World baseline** — chunks, block reads/writes, persistence boundary and client-visible updates.
5. **Simulation baseline** — movement validation, block interactions, inventory and player state.
6. **Scale validation** — repeatable synthetic load and real-client tests; tune from profiles, not guesses.

## Independence and licensing

High-level ideas and observable behavior are not a substitute for reviewing licenses. Do not copy Minestom source, mechanically rewrite it, or reproduce its distinctive implementation. Record design decisions independently, implement from first principles, track every dependency and its license, and review protocol specifications, assets and data under their applicable terms. A clean-room workflow can reduce risk but is not a legal guarantee.
