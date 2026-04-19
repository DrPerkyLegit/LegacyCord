package dev.drperky.networking.datatypes;

import dev.drperky.networking.NetworkManager;
import dev.drperky.networking.packets.LCEPacket;
import dev.drperky.networking.packets.PacketReader;

import java.nio.channels.SocketChannel;

public class PlayerConnection {
    SocketChannel clientChannel;
    PacketReader clientReader;

    SocketChannel serverChannel;
    PacketReader serverReader;

    ConnectionError pendingError;

    LCEPacket cachedPacket_PreLogin;
    LCEPacket cachedPacket_Login;

    String travelingHost;
    int travelingPort;
    boolean currentlyTraveling;
    boolean hasTraveled;
    boolean awaitingHandling;

    public PlayerConnection(SocketChannel client) {
        this.clientChannel = client;
        this.clientReader = new PacketReader(this);

        this.serverChannel = null;
        this.serverReader = new PacketReader(this);

        this.pendingError = ConnectionError.None;

        this.cachedPacket_PreLogin = null;
        this.cachedPacket_Login = null;

        this.travelingHost = "";
        this.travelingPort = 0;
        this.currentlyTraveling = false;
        this.hasTraveled = false;
        this.awaitingHandling = false;
    }

    public ConnectionError getPendingError() {
        return this.pendingError;
    }

    public void setPendingError(ConnectionError error) {
        this.pendingError = error;
    }

    public SocketChannel getServerChannel() { return this.serverChannel; }
    public SocketChannel getClientChannel() { return this.clientChannel; }

    public PacketReader getServerReader() { return this.serverReader; }
    public PacketReader getClientReader() { return this.clientReader; }

    public void setConnectedServer(SocketChannel server) {
        this.serverReader.clear();
        this.serverChannel = server;
    }

    //server transfer apis

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

    public boolean isConnectedToServer() {
        return this.serverChannel != null;
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
        this.currentlyTraveling = true;
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
