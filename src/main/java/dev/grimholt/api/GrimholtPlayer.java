package dev.grimholt.api;
import java.util.UUID;
public interface GrimholtPlayer { UUID uuid(); String name(); Position position(); void sendMessage(String message); void kick(String reason); }
