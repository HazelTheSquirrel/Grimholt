package dev.grimholt.server.api;
import dev.grimholt.api.CommandSender; import net.kyori.adventure.text.Component;
public final class MinestomCommandSender implements CommandSender {private final net.minestom.server.command.CommandSender sender;public MinestomCommandSender(net.minestom.server.command.CommandSender s){sender=s;}public String name(){return sender.getClass().getSimpleName();}public boolean isPlayer(){return sender instanceof net.minestom.server.entity.Player;}public void sendMessage(String m){sender.sendMessage(Component.text(m));}}
