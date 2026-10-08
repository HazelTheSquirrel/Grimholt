# Security and operational hardening

## Existing foundations

- Plugin metadata and dependency ordering validation.
- Dedicated plugin class loaders and cleanup paths.
- Bounded scheduler admission.
- Centralized security limits.
- Persistence path validation and atomic-file primitives.
- No permitted Bukkit/Spigot/Paper/Folia runtime dependency.

These are useful controls, not proof that the server is production-secure.

## Required hardening

- Strict VarInt, UTF-8, NBT and packet-length validation in every protocol state.
- Per-connection byte/packet limits, timeouts and bounded work queues.
- Fuzz malformed packets and authentication failure paths.
- Rate-limit expensive login, command, chunk and plugin-triggered operations.
- Bound generation, storage, region handoff and plugin work.
- Validate file paths and NBT before allocating or writing.
- Crash/failure injection for persistence and startup/shutdown.
- Audit dependency versions, licenses and vulnerabilities.
- Add tick watchdog, resource metrics and suspicious queue-growth alerts.
- Test listener/task cleanup, class-loader leaks and repeated enable/disable.

## Plugin trust boundary

This is **not a Java sandbox**. Plugins execute trusted server-side code and can consume resources or access process capabilities. Do not run untrusted plugins. API quotas and timeouts improve observability but cannot safely sandbox arbitrary Java.

## Incident readiness

Before release, document secure defaults, online/offline mode implications, backups, log retention, update/rollback, vulnerability reporting and recovery from corrupt world data.
