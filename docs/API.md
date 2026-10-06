# Grimholt public API

The public API lives under dev.grimholt.api.

## Core surfaces

- GrimholtServer: server-facing capability surface.
- GrimholtPlayer / GrimholtWorld: gameplay objects without Minestom types.
- EventBus / Event / EventListener: plugin event model.
- Scheduler / Task: bounded scheduling model.
- ServiceRegistry: explicit capability/service ownership.
- Command / CommandSender: command abstraction.
- GrimholtPlugin / GrimholtPluginContext: plugin lifecycle and stable logging facade.
- PluginManager / PluginDescriptor: discovery and dependency metadata.

## Compatibility rule

A public API type must not import or return Minestom, SLF4J, Adventure or other server-implementation classes. Internal adapters may use those libraries freely as long as implementation types do not cross the boundary. Plugin code must never be required to assume one global server thread.

## Lifecycle

Plugins receive onLoad, then onEnable. Disable happens in reverse load order. A plugin that fails during enable is disabled and the server startup is aborted.
