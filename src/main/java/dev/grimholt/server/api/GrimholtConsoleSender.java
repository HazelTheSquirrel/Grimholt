package dev.grimholt.server.api;

import dev.grimholt.api.CommandSender;
import java.util.Objects;
import java.util.function.Consumer;

public final class GrimholtConsoleSender implements CommandSender {
    private final Consumer<String> output;
    public GrimholtConsoleSender(Consumer<String> output) { this.output = Objects.requireNonNull(output); }
    @Override public String name() { return "CONSOLE"; }
    @Override public boolean isPlayer() { return false; }
    @Override public void sendMessage(String message) { output.accept(message); }
}
