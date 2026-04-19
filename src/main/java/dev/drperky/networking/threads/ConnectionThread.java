package dev.drperky.networking.threads;

import dev.drperky.networking.datatypes.ConnectionError;
import dev.drperky.networking.datatypes.PlayerConnection;
import dev.drperky.networking.NetworkManager;
import dev.drperky.networking.packets.LCEPacket;
import dev.drperky.utils.Logger;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConnectionThread extends Thread {

    private final Set<PlayerConnection> assignedPlayerConnections = new HashSet<>();
    ByteBuffer networkBuffer = ByteBuffer.allocate(8192);
    private final NetworkManager _netManager;

    public ConnectionThread(NetworkManager _netManager) {
        this._netManager = _netManager;
    }

    public void assignNewConnection(PlayerConnection connection) {
        synchronized (assignedPlayerConnections) {
            this.assignedPlayerConnections.add(connection);
        }
    }

    public int getAssignedClientCount() {
        synchronized (assignedPlayerConnections) {
            return this.assignedPlayerConnections.size();
        }
    }

    public void run() {
        List<PlayerConnection> staleConnections = new ArrayList<>();
        while (!Thread.currentThread().isInterrupted()) {

            //we snapshot so other threads can access assignedClientConnections still
            List<PlayerConnection> localConnections;
            synchronized (assignedPlayerConnections) {
                if (!staleConnections.isEmpty()) {
                    for (PlayerConnection connection : staleConnections) {
                        assignedPlayerConnections.remove(connection);
                    }
                }

                localConnections = new ArrayList<>(assignedPlayerConnections); //connection snapshot
            }

            if (!staleConnections.isEmpty()) {
                for (PlayerConnection connection : staleConnections) {
                    _netManager.handleClosingConnection(connection);
                }

                staleConnections.clear();
            }

            for (PlayerConnection connection : localConnections) {
                try {
                    if (connection.isAwaitingHandling()) {
                        NetworkThread.transferServer(connection);
                        connection.finishHandle();
                    }
                } catch(Exception e) { }
                try {
                    networkBuffer.clear();
                    if (connection.getClientReader().hasTooManyStreamErrors()) {
                        connection.setPendingError(ConnectionError.StreamError);
                        throw new Exception("Client Has Too Many Stream Errors");
                    }

                    {
                        int size = connection.getClientChannel().read(networkBuffer);
                        if (size > 0) {
                            networkBuffer.flip();
                            _netManager.handleIncomingStream(connection, networkBuffer, true);
                            networkBuffer.clear();
                        } else if (size == -1) {
                            connection.setPendingError(ConnectionError.ClientClosed);
                            throw new Exception("Client Socket Closed");
                        }
                    }

                    if (connection.isConnectedToServer()) {
                        int size = connection.getServerChannel().read(networkBuffer);
                        if (size > 0) {
                            networkBuffer.flip();
                            _netManager.handleIncomingStream(connection, networkBuffer, false);
                            networkBuffer.clear();
                        } else if (size == -1) {
                            if (!connection.isTraveling()) {
                                connection.setPendingError(ConnectionError.ServerClosed);
                                throw new Exception("Server Socket Closed");
                            }
                        }
                    }

                } catch (Exception e) {
                    staleConnections.add(connection);
                }
            }

            try { sleep(1); }
            catch (InterruptedException e) { throw new RuntimeException(e); }
        }
    }
}
