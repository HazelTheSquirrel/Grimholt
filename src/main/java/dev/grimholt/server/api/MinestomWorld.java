package dev.grimholt.server.api;
import dev.grimholt.api.*; import net.minestom.server.instance.Instance; import java.util.List;
public final class MinestomWorld implements GrimholtWorld {private final Instance instance;public MinestomWorld(Instance i){instance=i;}public Instance internal(){return instance;}public java.util.UUID id(){return instance.getUuid();}public String dimension(){return instance.getDimensionName();}public List<GrimholtPlayer> players(){return instance.getPlayers().stream().map(MinestomPlayer::new).map(x->(GrimholtPlayer)x).toList();}}
