package dev.drperky;

import dev.drperky.events.core.EventBus;
import dev.drperky.networking.NetworkManager;
import dev.drperky.utils.Logger;

import static java.lang.Thread.sleep;

public class LegacyCord {
    private static NetworkManager _netManager;
    private final static EventBus eventBus = new EventBus();

    public static void main(String[] args) {
        Logger.Info("Loading LegacyCord");
        //load config here

        Logger.Info("Starting NetworkManager");
        _netManager = new NetworkManager();

        while (_netManager.isTicking()) {
            try {
                sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        Logger.Info("Exiting LegacyCord...");
    }

    public static NetworkManager getNetworkManager() {
        return _netManager;
    }

    public static EventBus getEventBus() {
        return eventBus;
    }

}