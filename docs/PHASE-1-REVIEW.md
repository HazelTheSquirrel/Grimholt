# Phase 1 Review

## Scope

Phase 1 establishes the executable foundation. Vanilla parity, online authentication, full plugin loading, persistence and protocol completeness remain later phases.

## Implemented

- Gradle Java application project.
- Java 25 toolchain.
- Pinned Minestom 2026.10.05-26.2, SLF4J 2.0.20 and JUnit 6.1.3.
- Unit and integration test infrastructure.
- Grimholt bootstrap entry point.
- Configuration creation/loading/validation with versioning.
- Explicit NEW -> STARTING -> RUNNING -> STOPPING -> STOPPED/FAILED lifecycle.
- Idempotent shutdown paths for NEW and STOPPED.
- Internal Minestom adapter.
- Internal plugin boundary reserved for Phase 2.
- Bootstrap logging.
- GitHub Actions build definition pinned to Java 25 and Gradle 9.7.

## Pre-change review

Reviewed the master work plan, architecture, dependency policy, roadmap, current main tree, recent commits and the current Minestom 26.2 API/source. Minestom's current release documents Java 25 and the ServerProperties-era API.

## Adversarial second pass

Checked:

- double-start rejection;
- repeated stop behavior;
- startup failure cleanup;
- malformed integer configuration;
- invalid port range;
- unsupported configuration version;
- Minestom adapter double initialization;
- adapter cleanup after failed start;
- shutdown hook behavior;
- public Grimholt classes for Minestom type leakage;
- repository search for Bukkit, Spigot, Paper and Folia dependency names.

A compile issue in exception propagation was found during this pass and fixed before declaring the work ready for verification.

## Verification status: BLOCKED

The repository currently exposes the CI workflow, but GitHub Actions reports zero workflow runs for the repository. Therefore a real Gradle compile/test result cannot honestly be claimed from the GitHub connector.

Phase 1 must remain BLOCKED until the build and integration tests have actually executed successfully. Static review alone is not sufficient evidence for the phase exit gate.

## Known verification commands

    gradle --no-daemon clean test
    gradle --no-daemon assemble

Expected runtime smoke test:

    gradle run

The CI workflow pins Java 25 and Gradle 9.7 for the same verification path.
