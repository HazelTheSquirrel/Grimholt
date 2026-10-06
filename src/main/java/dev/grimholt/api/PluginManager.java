package dev.grimholt.api;
import java.nio.file.Path; import java.util.List;
public interface PluginManager { void discover(Path directory); void loadAll(); void enableAll(); void disableAll(); List<PluginDescriptor> plugins(); }
