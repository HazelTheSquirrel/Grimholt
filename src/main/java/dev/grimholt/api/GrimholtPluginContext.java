package dev.grimholt.api;

public interface GrimholtPluginContext {
    PluginDescriptor descriptor();
    GrimholtServer server();
    Scheduler scheduler();
    EventBus events();
    ServiceRegistry services();
    GrimholtLogger logger();
}
