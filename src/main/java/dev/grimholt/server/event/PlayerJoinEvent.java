package dev.grimholt.server.event;
import dev.grimholt.api.Event; import dev.grimholt.api.GrimholtPlayer;
public record PlayerJoinEvent(GrimholtPlayer player) implements Event {}
