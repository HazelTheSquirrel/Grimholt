# Grimholt public API

The public API is under `dev.grimholt.api`. It is a foundation, not yet a claim that all vanilla gameplay capabilities are exposed or stable.

## Existing API surfaces

- Server/player/world abstractions: `GrimholtServer`, `GrimholtPlayer`, `GrimholtWorld`, `Position`, `ServerState`.
- Plugins: `GrimholtPlugin`, `GrimholtPluginContext`, `PluginDescriptor`, `PluginManager`.
- Events: `Event`, `EventBus`, `EventListener`.
- Scheduling: `Scheduler`, `Task`.
- Commands: `Command`, `CommandSender`.
- Services and logging: `ServiceRegistry`, `GrimholtLogger`.

## Current boundaries and gaps

The API contains no Minestom types. That is necessary but not sufficient for a production API. Public contracts still need explicit compatibility/versioning rules, permissions, stable world/chunk/block/item/inventory/entity APIs, region-aware scheduling, event ownership and cleanup, plugin resource policy, and tested shutdown/failure behavior.

## Rules for future API changes

1. Plugins compile against the public API only, never server implementation packages.
2. Do not expose Minecraft implementation or third-party types unless an intentional compatibility commitment is documented.
3. Do not promise one universal gameplay thread. Every API operation must document its owner, async behavior and permitted handoff.
4. API additions need API tests, compatibility review, Javadocs and a working example.
5. Do not freeze APIs for mechanics that have not yet achieved stable runtime ownership.
6. Plugin code is trusted code, not sandboxed code.

## Lifecycle evidence

The current manager has descriptor/discovery/dependency/lifecycle foundations. Before production readiness, verify load/enable/disable ordering, partial failure cleanup, listener/task deregistration, class-loader closure, dependency-cycle handling and repeatable start/stop with executable tests.
