# Dependency Policy

## Hard bans

Grimholt must not depend on:
- Bukkit
- Spigot
- Paper
- Folia

This includes direct dependencies and accidental transitive dependencies.

## Minestom

Minestom is no longer a runtime dependency of Grimholt.

It may be consulted as a behavioral/architectural reference, but Grimholt implementation code must be original and Grimholt-owned. No Minestom type may cross a Grimholt public API boundary.

The standalone runtime must remain buildable and executable without the external `net.minestom:minestom` Maven artifact.

## Review checklist

For every dependency:
- Why is it needed?
- Can the JDK or existing Minestom stack provide it?
- Is it thread-safe under the intended usage?
- What is its allocation profile?
- What is its license?
- Does it expose native/platform requirements?
- Does it create an API compatibility obligation?
