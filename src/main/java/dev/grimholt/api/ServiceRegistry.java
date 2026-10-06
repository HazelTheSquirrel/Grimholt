package dev.grimholt.api;
import java.util.Optional;
public interface ServiceRegistry { <T> void register(Class<T> type,T service); <T> Optional<T> find(Class<T> type); <T> T require(Class<T> type); <T> boolean unregister(Class<T> type,T expected); }
