# Subsystem inventory — baseline for Phase A

Audit ref: `test` at `950a4bbeef28348ad7f13bf600878afe62afc7bf` (inventory authored against this source tree; update the ref and re-audit when implementation changes).

This is a source-to-runtime inventory, not a vanilla-completeness assertion. A Java class or unit test only proves the specific behavior covered by that test. The active target is Minecraft Java Edition **26.4 Snapshot 3**; classes with `26_2` names are historical implementation names and are not evidence that 26.2 is the active target.

| Source area | Main runtime entry / integration | Existing test evidence | Remaining baseline gaps |
|---|---|---|---|
| `dev.grimholt.api` | Public API contracts; wired by `Grimholt` through `GrimholtServerImpl` | API/scheduler and integration tests | API versioning, capability completeness, compatibility policy |
| `server.lifecycle`, `server.config`, `server.logging` | `Grimholt.main → start → stop` | `LifecycleTest`, `ConfigLoaderTest`, integration test | Full production config validation, process-level start/stop smoke |
| `server.network` | `GrimholtNetworkServer → GrimholtConnection → PacketTransport` | Cipher, authentication, inventory and codec tests; protocol handshake tests in vanilla package | End-to-end 26.4 client path through configuration and Play is not proven |
| `server.command`, `server/vanilla/VanillaCommandDispatcher` | Dispatcher is constructed by `Grimholt` and passed to network | Command tree/protocol coverage is limited | Full Brigadier-compatible grammar, command set and permission semantics |
| `server.concurrency`, `server.runtime` | `GrimholtRegionTickEngine`, `VanillaServerKernel`, bounded scheduler | Ownership, region, scheduler and tick tests | Full state ownership, bounded handoffs, races and load/soak evidence |
| `server.world` | `GrimholtChunkTransport` connects chunk output to network | Chunk wire codec tests | Authoritative chunk lifecycle, generated terrain, lighting and client render parity |
| `server.entity`, `server/vanilla/*Entity*` | Entity lifecycle/model and packet helpers | Limited kernel/gameplay tests | Full entity registry, metadata, tracking, AI, persistence and vanilla parity |
| `server/vanilla` | `VanillaServerKernel` owns available gameplay/model engines | Gameplay kernel, tick, chunk, snapshot and parity infrastructure tests | Broadly incomplete vanilla coverage; do not infer feature parity from class count |
| `server.persistence`, `VanillaAnvilRegion`, `VanillaPersistenceModel` | Atomic file/path primitives and region model | `AtomicFileStoreTest`, selected model tests | Full Anvil/level/player/entity/POI data, migration and crash-recovery proof |
| `server.plugin` | `PluginBoundary` and `PluginLoader` called during startup | Integration/API tests | API compatibility, class-loader cleanup, plugin failure isolation and quotas |
| `server.metrics`, `server.ops`, `server.security` | Metrics and security limits registered at startup; health-check types available | Health, resource profile and security-related tests | Live tick/network telemetry, rate limiting proof, alerts and long soak |
| `server/vanilla/VanillaGenerated*` | Gradle data generation consumes pinned Mojang reference; runtime loader/catalog classes | Generated-data tests | Prove each generated report is consumed by the right runtime systems; full registries/data parity |
| `reference/minecraft/26.4/server.jar` | Used by checksum, report generation and isolated reference smoke tasks only | CI checksum and reference smoke task | Must remain excluded from Grimholt runtime artifact |
| `src/test/java` | JUnit suite executed by Gradle `test` | CI runs clean test suite | Differential, fuzz, real-client, crash-injection and networked scale tests remain separate gates |

## Build and independence gates

- `dependencyAudit` rejects forbidden Minecraft implementation/API dependency groups in the resolved runtime graph.
- `forkIntegrityAudit` additionally scans main Java imports and inspects the standalone JAR for forbidden package entries, reference JAR inclusion, the Grimholt entry point and manifest.
- CI uses a clean hosted checkout, verifies the pinned reference checksum, runs `clean test`, boots the Mojang reference as a smoke test, assembles the standalone artifact and runs the integrity audit.
- These gates establish build/dependency packaging integrity; they do **not** establish a real-client handshake beyond unit-tested pieces or vanilla gameplay parity.

## Phase-A closure evidence

Do not mark Phase A complete until the integrity-audit workflow on the exact current `test` head is green and its artifact upload succeeds. Record the resulting workflow run URL in the master work plan or release notes.
