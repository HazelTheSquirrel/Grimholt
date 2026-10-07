package dev.grimholt.server.api;

import dev.grimholt.api.GrimholtPlayer;
import dev.grimholt.api.Position;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class GrimholtPlayerImpl implements GrimholtPlayer {
    private final UUID uuid;
    private final String name;
    private final Consumer<String> messageSink;
    private final Consumer<String> kickSink;
    private final AtomicReference<Position> position =
            new AtomicReference<>(new Position(0, 64, 0, 0, 0));

    public GrimholtPlayerImpl(UUID uuid, String name,
                              Consumer<String> messageSink,
                              Consumer<String> kickSink) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.name = Objects.requireNonNull(name, "name");
        this.messageSink = Objects.requireNonNull(messageSink, "messageSink");
        this.kickSink = Objects.requireNonNull(kickSink, "kickSink");
    }

    @Override public UUID uuid() { return uuid; }
    @Override public String name() { return name; }
    @Override public Position position() { return position.get(); }

    public void position(Position value) { position.set(Objects.requireNonNull(value, "value")); }

    @Override public void sendMessage(String message) {
        messageSink.accept(Objects.requireNonNull(message, "message"));
    }

    @Override public void kick(String reason) {
        kickSink.accept(Objects.requireNonNull(reason, "reason"));
    }
}
