package dev.drperky;

import dev.drperky.events.core.EventBus;
import dev.drperky.networking.NetworkManager;
import dev.drperky.utils.Config;
import dev.drperky.utils.Logger;

import java.io.File;
import java.net.URISyntaxException;

import static java.lang.Thread.sleep;

public class LegacyCord {
    private static NetworkManager _netManager;
    private final static EventBus eventBus = new EventBus();

    public static void main(String[] args) {
        Logger.Info("Loading LegacyCord");
        Config proxyConfig = Config.load(LegacyCord.getRunningJar());

        Logger.Info("Starting NetworkManager");
        _netManager = new NetworkManager(proxyConfig.serverAddress, proxyConfig.serverPort, proxyConfig.proxyPort);

        while (_netManager.isTicking()) {
            try {
                sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        Logger.Info("Exiting LegacyCord...");
    }

    private static File getRunningJar() {
        try {
            return new File(LegacyCord.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static NetworkManager getNetworkManager() {
        return _netManager;
    }

    public static EventBus getEventBus() {
        return eventBus;
    }

}