package dev.drperky.networking.datatypes;

public class ServerTravelData {
    LCEPacket cachedPacket_PreLogin;
    LCEPacket cachedPacket_Login;

    String travelingHost;
    int travelingPort;
    boolean currentlyTraveling;
    boolean hasTraveled;
    boolean awaitingHandling;

    ServerTravelData() {
        this.cachedPacket_PreLogin = null;
        this.cachedPacket_Login = null;

        this.travelingHost = "";
        this.travelingPort = 0;
        this.currentlyTraveling = false;
        this.hasTraveled = false;
        this.awaitingHandling = false;
    }

    public void setCachedPacket_PreLogin(LCEPacket packet) {
        cachedPacket_PreLogin = packet.clone();
    }

    public LCEPacket getCachedPacket_PreLogin() {
        return cachedPacket_PreLogin;
    }

    public void setCachedPacket_Login(LCEPacket packet) {
        cachedPacket_Login = packet.clone();
    }

    public LCEPacket getCachedPacket_Login() {
        return cachedPacket_Login;
    }

    public boolean isTraveling() {
        return this.currentlyTraveling;
    }

    public boolean hasTraveledOnce() {
        return this.hasTraveled;
    }

    public boolean isAwaitingHandling() {
        return this.awaitingHandling;
    }

    public void finishHandle() {
        this.currentlyTraveling = true;
        this.awaitingHandling = false;
    }

    public String getQueuedTravelHost() {
        return travelingHost;
    }

    public int getQueuedTravelPort() {
        return travelingPort;
    }

    public void queueTravel(String host, int port) {
        this.awaitingHandling = true;
        this.currentlyTraveling = false; // was true;
        this.travelingHost = host;
        this.travelingPort = port;
    }

    public void finishTraveling() {
        this.hasTraveled = true;
        this.currentlyTraveling = false;
        this.travelingHost = "";
        this.travelingPort = 0;
    }
}
