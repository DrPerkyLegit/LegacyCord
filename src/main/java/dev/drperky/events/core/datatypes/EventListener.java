package dev.drperky.events.core.datatypes;

public interface EventListener<T extends Event> {
    void handle(T event);
}
