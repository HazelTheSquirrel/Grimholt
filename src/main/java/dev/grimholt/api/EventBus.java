package dev.grimholt.api;
public interface EventBus { <E extends Event> void register(Class<E> type,EventListener<? super E> listener); <E extends Event> boolean unregister(Class<E> type,EventListener<? super E> listener); <E extends Event> void post(E event); }
