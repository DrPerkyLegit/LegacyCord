package dev.drperky.events.core.datatypes;

public interface Cancelable {
    boolean isCancelled();
    void setCancelled(boolean cancelled);
}
