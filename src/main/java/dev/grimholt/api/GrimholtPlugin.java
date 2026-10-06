package dev.grimholt.api;
public interface GrimholtPlugin { default void onLoad(GrimholtPluginContext context) {} default void onEnable() {} default void onDisable() {} }
