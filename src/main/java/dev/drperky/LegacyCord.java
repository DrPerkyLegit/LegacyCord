package dev.drperky;

import dev.drperky.events.core.EventBus;
import dev.drperky.events.core.datatypes.EventListener;
import dev.drperky.events.core.datatypes.PacketEvent;
import dev.drperky.networking.NetworkManager;
import dev.drperky.networking.datatypes.PlayerConnection;
import dev.drperky.utils.Logger;

import static java.lang.Thread.sleep;

public class LegacyCord {
    private final static EventBus eventBus = new EventBus();

    public static EventBus getEventBus() {
        return eventBus;
    }

    public static void main(String[] args) {
        Logger.Info("Loading LegacyCord");
        //load config here

        Logger.Info("Starting NetworkManager");
        NetworkManager _netManager = new NetworkManager();

        while (_netManager.isTicking()) {
            try {
                sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        Logger.Info("Exiting LegacyCord...");
    }
}