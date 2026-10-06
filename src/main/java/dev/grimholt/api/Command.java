package dev.grimholt.api;
import java.util.List;
public interface Command { String name(); List<String> aliases(); void execute(CommandSender sender, String[] arguments); default List<String> suggest(CommandSender sender, String[] arguments) { return List.of(); } }
