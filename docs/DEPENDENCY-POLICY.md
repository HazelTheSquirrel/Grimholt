# Dependency Policy

## Hard bans

Grimholt must not depend on:
- Bukkit
- Spigot
- Paper
- Folia

This includes direct dependencies and accidental transitive dependencies.

## Minestom

Minestom is permitted as the server implementation foundation.

However, Grimholt's public API must not become a thin re-export of Minestom.

Internal Minestom types should remain behind the Grimholt implementation boundary whenever practical.

## Review checklist

For every dependency:
- Why is it needed?
- Can the JDK or existing Minestom stack provide it?
- Is it thread-safe under the intended usage?
- What is its allocation profile?
- What is its license?
- Does it expose native/platform requirements?
- Does it create an API compatibility obligation?
