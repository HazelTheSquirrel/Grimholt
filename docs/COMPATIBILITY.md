# Compatibility and parity policy

Grimholt targets Minecraft 26.2 and uses Minestom internally for protocol, registry and low-level server facilities. This does not mean Bukkit/Paper/Folia compatibility, and no public Grimholt API exposes Minestom classes.

## What is currently proven
- Minecraft 26.2 protocol bootstrap through Minestom.
- Offline authentication bootstrap for development/integration tests.
- Player configuration, spawn and disconnect lifecycle.
- A default overworld instance.
- Independent plugin descriptors, dependency ordering, classloader isolation and lifecycle.
- Independent Grimholt API interfaces for players, worlds, commands, events, services and scheduling.
- Atomic persistence primitive and bounded asynchronous scheduler.

## Explicitly not claimed yet
- Full vanilla gameplay/mechanics parity with the official 26.2 server.
- Online-mode authentication parity.
- Vanilla world generation parity.
- Complete redstone/fluid/AI/raid/villager/combat edge-case parity.
- 500-1000 player benchmark results on representative production hardware.

A feature is only marked compatible after an executable test or benchmark demonstrates it. Minestom delegation is implementation detail, not evidence of vanilla parity.
