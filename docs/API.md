# Grimholt public API

The public API lives under dev.grimholt.api.

## Core surfaces

- GrimholtServer: server-facing capability surface.
- GrimholtPlayer / GrimholtWorld: gameplay objects without Minestom types.
- EventBus / Event / EventListener: plugin event model.
- Scheduler / Task: bounded scheduling model.
- ServiceRegistry: explicit capability/service ownership.
- Command / CommandSender: command abstraction.
- GrimholtPlugin / GrimholtPluginContext: plugin lifecycle.
- PluginManager / PluginDescriptor: discovery and dependency metadata.

## Compatibility rule

A public API type must not import or return a Minestom class. Internal adapters may use Minestom freely as long as the type does not cross the boundary.

## Lifecycle

Plugins receive onLoad, then onEnable. Disable happens in reverse load order. A plugin that fails during enable is disabled and the server startup is aborted.
