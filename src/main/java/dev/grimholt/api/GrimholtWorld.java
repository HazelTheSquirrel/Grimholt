package dev.grimholt.api;
import java.util.List; import java.util.UUID;
public interface GrimholtWorld { UUID id(); String dimension(); List<GrimholtPlayer> players(); }
