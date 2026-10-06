# Grimholt

Grimholt is an independent Minecraft server fork/distribution built on top of Minestom.

## Goals

- Vanilla Minecraft gameplay and protocol compatibility for the targeted Minecraft release.
- No Bukkit, Spigot, Paper or Folia runtime/API dependency.
- A first-class, stable Grimholt Plugin API.
- Strong separation between server internals and public plugin API.
- Architecture designed for horizontal work distribution and high concurrency.
- Performance target: 500-1000 concurrent players, subject to hardware, world activity and gameplay workload.

## Current status

Engineering foundation + active Vanilla parity implementation. The behavioral target is Minecraft Java 26.4 Snapshot 3; the current Minestom substrate remains 26.2 until an upstream compatible runtime is available. Grimholt implements Vanilla behavior in its own code and never modifies Minestom.

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




