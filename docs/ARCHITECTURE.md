# Grimholt Architecture

## Core principle

Grimholt is a server implementation, not a Bukkit compatibility layer.

The public dependency direction is:

Minecraft client
  -> Grimholt server
  -> Grimholt API
  -> Grimholt plugins

Server internals may use Minestom. Plugins must depend on the Grimholt API and explicitly documented stable libraries only.

## Boundary layers

### 1. Bootstrap
Owns process startup, configuration, logging and shutdown.

### 2. Server kernel
Owns lifecycle, tick scheduling, world ownership, player ownership, entity ownership and coordination.

### 3. Minecraft implementation
Owns protocol-facing behavior and vanilla gameplay.

### 4. Persistence
Owns world/player/configuration persistence and serialization boundaries.

### 5. Plugin platform
Owns plugin discovery, class loading, dependency resolution, lifecycle, events, commands, scheduling and services.

### 6. Public API
Contains only types that plugins are expected to compile against.

## Concurrency model

The design must avoid a single giant global lock.

State ownership must be explicit. Where state can be partitioned by world, chunk, player or subsystem, ownership should be partitioned as well.

CPU-heavy work such as world generation, storage I/O and expensive calculations must be separated from latency-sensitive tick execution and must have bounded queues/backpressure.

The API must not force plugins to assume that every operation executes on one global server thread.

## Plugin safety

A plugin must never receive unrestricted references to internal implementation objects.

The API should expose capabilities, not implementation classes.

Examples:

- Public `Player` interface instead of internal player implementation.
- Public `World` interface instead of internal world implementation.
- Public event contracts instead of direct event-bus internals.
- Public scheduler abstraction instead of executor internals.

## Performance rules

Do not optimize from intuition.

Every major subsystem should eventually have:
- a functional test,
- a regression benchmark where appropriate,
- allocation awareness,
- contention awareness,
- a documented ownership model.

The 500-1000 player target is treated as a workload requirement that drives architecture, not as a marketing number.

## Dependency policy

Forbidden runtime dependencies:
- Bukkit
- Spigot
- Paper
- Folia

Minestom is an implementation foundation, not a plugin API.

Every new dependency must have:
- a concrete reason,
- license compatibility,
- maintenance assessment,
- performance impact assessment,
- security impact assessment.

## Compatibility policy

The targeted Minecraft version is the source of truth for protocol and vanilla behavior.

Do not silently implement behavior from another Minecraft version merely because it is convenient.

## Definition of done

A feature is done only when:
1. implementation exists,
2. tests exist where meaningful,
3. failure modes were reviewed,
4. dependency boundaries were checked,
5. concurrency implications were checked,
6. the feature was re-reviewed after implementation.
