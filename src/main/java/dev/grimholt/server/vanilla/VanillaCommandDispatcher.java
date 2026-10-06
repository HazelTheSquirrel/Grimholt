package dev.grimholt.server.vanilla;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class VanillaCommandDispatcher<S> {
    private final Map<String,BiConsumer<S,String[]>> commands=new ConcurrentHashMap<>();
    public void register(String literal,BiConsumer<S,String[]> executor){if(literal==null||literal.isBlank()||executor==null)throw new IllegalArgumentException();commands.put(literal.toLowerCase(java.util.Locale.ROOT),executor);}
    public boolean execute(S sender,String input){String[] a=input.trim().split("\\s+");if(a.length==0||a[0].isBlank())return false;var c=commands.get(a[0].startsWith("/")?a[0].substring(1).toLowerCase(java.util.Locale.ROOT):a[0].toLowerCase(java.util.Locale.ROOT));if(c==null)return false;c.accept(sender,java.util.Arrays.copyOfRange(a,1,a.length));return true;}
    public boolean contains(String literal){return commands.containsKey(literal.toLowerCase(java.util.Locale.ROOT));}
}
