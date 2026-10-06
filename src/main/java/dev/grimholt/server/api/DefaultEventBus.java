package dev.grimholt.server.api;

import dev.grimholt.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.concurrent.*;

public final class DefaultEventBus implements EventBus {
    private static final Logger LOGGER=LoggerFactory.getLogger(DefaultEventBus.class);
    private final ConcurrentHashMap<Class<?>,CopyOnWriteArrayList<EventListener<?>>> listeners=new ConcurrentHashMap<>();
    public <E extends Event> void register(Class<E> type,EventListener<? super E> listener){listeners.computeIfAbsent(type,k->new CopyOnWriteArrayList<>()).addIfAbsent(listener);}
    public <E extends Event> boolean unregister(Class<E> type,EventListener<? super E> listener){var list=listeners.get(type);return list!=null&&list.remove(listener);}
    @SuppressWarnings("unchecked") public <E extends Event> void post(E event){var list=listeners.get(event.getClass());if(list==null)return;for(var listener:List.copyOf(list)){try{((EventListener<E>)listener).onEvent(event);}catch(Throwable failure){LOGGER.error("Plugin event listener failed for {}",event.getClass().getName(),failure);}}}
    public void clear(){listeners.clear();}
}
