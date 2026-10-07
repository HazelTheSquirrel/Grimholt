package dev.grimholt.server.command;

import dev.grimholt.api.Command;
import dev.grimholt.api.CommandSender;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class GrimholtCommandDispatcher {
    private final Map<String, Command> commands = new ConcurrentHashMap<>();

    public void register(Command command) {
        Objects.requireNonNull(command, "command");
        String name = normalize(command.name());
        if (name.isEmpty()) throw new IllegalArgumentException("Command name is blank");
        if (commands.putIfAbsent(name, command) != null) throw new IllegalArgumentException("Command already registered: " + name);
        for (String alias : command.aliases()) {
            String key = normalize(alias);
            if (!key.isEmpty() && commands.putIfAbsent(key, command) != null) {
                commands.remove(name, command);
                throw new IllegalArgumentException("Command alias already registered: " + key);
            }
        }
    }

    public boolean execute(CommandSender sender, String input) {
        Objects.requireNonNull(sender, "sender");
        String[] tokens = tokenize(input);
        if (tokens.length == 0) return false;
        Command command = commands.get(normalize(tokens[0]));
        if (command == null) return false;
        command.execute(sender, Arrays.copyOfRange(tokens, 1, tokens.length));
        return true;
    }

    public List<String> suggest(CommandSender sender, String input) {
        String[] tokens = tokenize(input);
        if (tokens.length == 0) return commands.keySet().stream().sorted().toList();
        Command command = commands.get(normalize(tokens[0]));
        if (command == null) {
            String prefix = normalize(tokens[0]);
            return commands.keySet().stream().filter(x -> x.startsWith(prefix)).sorted().toList();
        }
        return command.suggest(sender, Arrays.copyOfRange(tokens, 1, tokens.length));
    }

    public int size() { return commands.size(); }

    public List<String> registeredNames() { return commands.keySet().stream().sorted().toList(); }

    private static String[] tokenize(String input) {
        if (input == null) return new String[0];
        String value = input.trim();
        if (value.startsWith("/")) value = value.substring(1);
        if (value.isBlank()) return new String[0];
        return value.split("\\s+");
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String v = value.trim();
        if (v.startsWith("/")) v = v.substring(1);
        return v.toLowerCase(Locale.ROOT);
    }
}
