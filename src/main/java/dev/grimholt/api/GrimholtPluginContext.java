package dev.grimholt.api;
import org.slf4j.Logger;
public interface GrimholtPluginContext { PluginDescriptor descriptor(); GrimholtServer server(); Scheduler scheduler(); EventBus events(); ServiceRegistry services(); Logger logger(); }
