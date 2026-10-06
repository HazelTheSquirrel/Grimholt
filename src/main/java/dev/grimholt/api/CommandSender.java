package dev.grimholt.api;
public interface CommandSender { String name(); boolean isPlayer(); void sendMessage(String message); }
