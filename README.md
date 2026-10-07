# Grimholt

Grimholt is an independent Minecraft server implementation/distribution built on a replaceable Minestom substrate.

## Goals

- Vanilla Minecraft gameplay and protocol compatibility for the targeted Minecraft release.
- No Bukkit, Spigot, Paper or Folia runtime/API dependency.
- A first-class, stable Grimholt Plugin API.
- Strong separation between server internals and public plugin API.
- Architecture designed for horizontal work distribution and high concurrency.
- Performance target: 500-1000 concurrent players, subject to hardware, world activity and gameplay workload.

## Current status

Engineering foundation + active Vanilla parity implementation. **Minecraft parity is not complete yet.** The repository contains the ownership kernel and a growing set of Grimholt-owned vanilla systems; protocol compatibility provided by Minestom is not counted as vanilla parity. The behavioral target is Minecraft Java 26.2. Minestom is deliberately pinned as a low-level substrate and is not the source of Grimholt's vanilla behavior. Grimholt implements the 26.4 behavior, registries, world model, tick model and protocol contract in its own code; Minestom is only a temporary compatibility transport until Grimholt's native 26.4 transport replaces it. We do not modify Minestom. The server artifact is self-contained: operators do not install a separate Minestom or Minecraft server JAR.

This repository is intentionally being built in phases. A phase is not considered complete until its implementation and a separate verification pass agree.

## Engineering rule

Never declare a phase complete merely because the code compiles. Each phase must pass:
1. implementation checks,
2. API/architecture review,
3. concurrency/performance review where applicable,
4. regression tests,
5. dependency audit,
6. a second adversarial review focused on failure modes.

Before every implementation change, follow `docs/MASTER-WORKPLAN.md`. It is the mandatory pre-change and post-change engineering checklist.

See `docs/MASTER-WORKPLAN.md`, `docs/COMPATIBILITY.md` and `docs/ARCHITECTURE.md`.





## Versioning rule

Minecraft and Minestom are intentionally versioned independently. A Minecraft update changes the pinned `VanillaSnapshot` and regenerated Mojang reference data; the Minestom substrate is not updated merely because Minecraft changes. If the substrate changes, that is a separate compatibility decision.

The exact target currently pinned is **Minecraft 26.2** (protocol `776`, world data version `4903`, Java 25). The generated Mojang reports are treated as reference input, not as a replacement for Grimholt-owned behavior.
