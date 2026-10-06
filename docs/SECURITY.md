# Security and operational hardening

- Public plugin API contains no Minestom implementation types.
- Plugin dependencies are validated before activation; hard dependency cycles fail deterministically.
- Plugin JARs use dedicated URL classloaders that are closed on disable.
- Scheduler admission is bounded; rejected work fails fast instead of growing without limit.
- Persistence writes are rooted and use atomic replacement where the filesystem supports it.
- Runtime limits are centralized in SecurityLimits.
- Malformed plugin metadata and invalid paths fail closed.
- No Bukkit/Spigot/Paper/Folia dependency is permitted.

This is not a Java security sandbox. Plugins are trusted server-side code and must be treated as such.
