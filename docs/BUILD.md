# Grimholt build and verification

## Toolchain

- JDK 25 (Temurin in CI)
- Gradle 9.8.0
- Mojang reference: `reference/minecraft/26.4/server.jar`
- Expected reference SHA-1: `2d89c95c030e635387448f332961074ce1adbb4b`

## Local commands

```bash
gradle --no-daemon clean test
gradle --no-daemon dependencyAudit
gradle --no-daemon vanillaReferenceSmoke26_4
gradle --no-daemon assemble
gradle --no-daemon benchmark
```

Run the reference generation task when changing generated data:
```bash
gradle --no-daemon generateVanilla26_4
``

The application entry point is `dev.grimholt.server.Grimholt`; `gradle run` starts it. Check the current configuration/bootstrap behavior before relying on production deployment options.

## CI coverage

The current workflow verifies the reference checksum, runs `clean test`, boots the official Mojang reference server and waits for its ready message, assembles the standalone JAR, and uploads the artifact. It does not yet run a Grimholt-vs-vanilla differential harness, a real-client login/gameplay test, fuzzing, persistence crash injection, or a networked 500–1000-player benchmark.

## Artifact and dependency checks

The `standaloneJar` task embeds resolved runtime dependencies into the server artifact. `dependencyAudit` blocks the listed Minestom/Bukkit/Spigot/Paper/Folia coordinates. Inspect the dependency graph and artifact contents whenever dependencies change; successful assembly alone is not proof of runtime independence.

## Reproducibility

The Mojang reference JAR is pinned by SHA-1. Generated reports are produced from that exact JAR; regenerate rather than manually editing generated output. Before release, add deterministic build verification, dependency/license reporting and a clean-checkout smoke test.
