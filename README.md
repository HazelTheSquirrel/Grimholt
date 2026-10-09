# Grimholt Server

Grimholt is an independent Minecraft server implementation in Java 25. The repository is being rebuilt around its own runtime, protocol, session, world, simulation, and storage boundaries. It does not embed or wrap another Minecraft server implementation.

## Status

This commit establishes the first independent core primitives and concurrency rules. **It is not yet a playable Minecraft server**: the Minecraft wire protocol, login/session flow, chunk streaming, gameplay simulation, and persistence are not implemented in this foundation stage. The bootstrap reports the detected runtime profile and exits; it does not open a game listener.

## Requirements

- JDK 25
- Maven 3.9+

## Build and run

    mvn --batch-mode clean verify
    java -jar target/grimholt-server-0.1.0-SNAPSHOT.jar

The packaged JAR is a runnable foundation diagnostic, not yet a production game server.

## Architecture

- **Transport:** socket lifecycle, packet framing, compression/encryption, and backpressure. This is a future module; no game listener is enabled yet.
- **Protocol:** version-specific codecs and explicit protocol-state transitions.
- **Session:** identity, login lifecycle, connection ownership, and ordered outbound delivery.
- **World:** primitive coordinate keys, chunk/block storage contracts, chunk lifecycle, and persistence boundaries.
- **Simulation:** authoritative game rules, driven by region ownership rather than shared mutable world state.
- **Runtime:** lifecycle, hardware-aware capacity defaults, bounded work queues, scheduling, and observability.
- **Storage:** asynchronous persistence and recovery.
- **Observability:** queue saturation, queue wait, allocation rate, and p50/p95/p99 simulation latency.

### Concurrency rules

1. Network workers will do bounded I/O and decoding only; gameplay work is routed to a bounded owner queue.
2. A mutable world region has one writer at a time. Cross-region changes are explicit messages.
3. Queue saturation is a visible result. Expensive work is never run on the submitting thread as a fallback.
4. Background work must have bounded concurrency and bounded queued work.
5. Allocation pooling is introduced only when profiling demonstrates a benefit.
6. Shutdown closes admission first, then drains or rejects queued work according to each subsystem's contract.

## Performance target

The design target is 500+ concurrent players in one world, but no capacity claim is made until repeatable load tests measure throughput, memory, queue wait, and p50/p95/p99 simulation latency on documented hardware.

## Build artifacts

A successful CI verification uploads the runnable foundation JAR as grimholt-server-<commit-sha> for 30 days. Open https://github.com/HazelTheSquirrel/Grimholt/actions, choose a successful CI run, and download its artifact ZIP.

## Independent implementation and dependencies

Core packages must not depend on third-party server runtimes. Dependencies and licenses are tracked explicitly; protocol behavior is implemented from documented wire specifications and independent tests rather than copied server source.
