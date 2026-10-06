package dev.grimholt.api;
@FunctionalInterface public interface EventListener<E extends Event> { void onEvent(E event); }
