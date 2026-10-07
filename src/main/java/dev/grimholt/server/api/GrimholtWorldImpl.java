package dev.grimholt.server.api;

import dev.grimholt.api.GrimholtPlayer;
import dev.grimholt.api.GrimholtWorld;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class GrimholtWorldImpl implements GrimholtWorld {
    private final UUID id;
    private final String dimension;
    private final Supplier<List<GrimholtPlayer>> players;

    public GrimholtWorldImpl(UUID id, String dimension, Supplier<List<GrimholtPlayer>> players) {
        this.id = Objects.requireNonNull(id, "id");
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.players = Objects.requireNonNull(players, "players");
    }

    @Override public UUID id() { return id; }
    @Override public String dimension() { return dimension; }
    @Override public List<GrimholtPlayer> players() { return List.copyOf(players.get()); }
}
