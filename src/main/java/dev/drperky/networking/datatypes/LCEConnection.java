package dev.drperky.networking.datatypes;

import dev.drperky.networking.PacketReader;

import java.nio.channels.SocketChannel;

public class LCEConnection {
    SocketChannel clientChannel;
    PacketReader clientReader;

    SocketChannel serverChannel;
    PacketReader serverReader;

    ConnectionError pendingError;

    ServerTravelData travelData;


    public LCEConnection(SocketChannel client) {
        this.clientChannel = client;
        this.clientReader = new PacketReader(this);

        this.serverChannel = null;
        this.serverReader = new PacketReader(this);

        this.pendingError = ConnectionError.None;

        this.travelData = new ServerTravelData();
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

    public boolean isConnectedToServer() {
        return this.serverChannel != null;
    }
    public void setConnectedServer(SocketChannel server) {
        this.serverReader.clear();
        this.serverChannel = server;
    }

    public ServerTravelData getTravelData() {
        return this.travelData;
    }
}
