# Dependency Policy

## Hard bans

Grimholt must not depend on:
- Bukkit
- Spigot
- Paper
- Folia

This includes direct dependencies and accidental transitive dependencies.

## Minestom

Minestom is currently permitted only as a temporary migration dependency/source foundation.

However, Grimholt's public API must not become a thin re-export of Minestom.

Required Minestom implementation code will progressively become Grimholt-owned, adapted or replaced.

The final standalone Grimholt runtime must not require the external `net.minestom:minestom` Maven artifact. Every remaining Minestom dependency needs a migration/removal path.

## Review checklist

For every dependency:
- Why is it needed?
- Can the JDK or existing Minestom stack provide it?
- Is it thread-safe under the intended usage?
- What is its allocation profile?
- What is its license?
- Does it expose native/platform requirements?
- Does it create an API compatibility obligation?
