package dev.drperky.events.core;

import dev.drperky.events.core.datatypes.Event;
import dev.drperky.events.core.datatypes.EventListener;
import dev.drperky.events.core.datatypes.PacketEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventBus {

    private final Map<Class<?>, List<EventListener<?>>> listeners = new HashMap<>();

    private final Map<Integer, List<EventListener<PacketEvent>>> packetListeners = new HashMap<>();

    public <T extends Event> void register(Class<T> type, EventListener<T> listener) {
        listeners.computeIfAbsent(type, k -> new ArrayList<>()).add(listener);
    }

    public <T extends Event> void registerPacketEvent(int packetId, EventListener<PacketEvent> listener) {
        packetListeners.computeIfAbsent(packetId, k -> new ArrayList<>()).add(listener);
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> void fire(T event) {
        if (event instanceof PacketEvent) {
            List<EventListener<PacketEvent>> list = packetListeners.get(((PacketEvent) event).packet.getPacketId());
            if (list == null) return;

            for (EventListener<PacketEvent> raw : list) {
                ((EventListener<T>) raw).handle(event);
            }
        } else {
            List<EventListener<?>> list = listeners.get(event.getClass());
            if (list == null) return;

            for (EventListener<?> raw : list) {
                ((EventListener<T>) raw).handle(event);
            }
        }

    }
}
