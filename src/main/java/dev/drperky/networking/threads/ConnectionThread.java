package dev.drperky.networking.threads;

import dev.drperky.networking.datatypes.ConnectionError;
import dev.drperky.networking.datatypes.LCEConnection;
import dev.drperky.networking.NetworkManager;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConnectionThread extends Thread {

    private final Set<LCEConnection> assignedLCEConnections = new HashSet<>();
    ByteBuffer networkBuffer = ByteBuffer.allocate(8192);
    private final NetworkManager _netManager;

    public ConnectionThread(NetworkManager _netManager) {
        this._netManager = _netManager;
    }

    public void assignNewConnection(LCEConnection connection) {
        synchronized (assignedLCEConnections) {
            this.assignedLCEConnections.add(connection);
        }
    }

    public int getAssignedClientCount() {
        synchronized (assignedLCEConnections) {
            return this.assignedLCEConnections.size();
        }
    }

    public void run() {
        List<LCEConnection> staleConnections = new ArrayList<>();
        while (!Thread.currentThread().isInterrupted()) {

            //we snapshot so other threads can access assignedClientConnections still
            List<LCEConnection> localConnections;
            synchronized (assignedLCEConnections) {
                if (!staleConnections.isEmpty()) {
                    for (LCEConnection connection : staleConnections) {
                        assignedLCEConnections.remove(connection);
                    }
                }

                localConnections = new ArrayList<>(assignedLCEConnections); //connection snapshot
            }

            if (!staleConnections.isEmpty()) {
                for (LCEConnection connection : staleConnections) {
                    _netManager.handleClosingConnection(connection);
                }

                staleConnections.clear();
            }

            for (LCEConnection connection : localConnections) {
                try {
                    if (connection.getTravelData().isAwaitingHandling()) {
                        NetworkThread.transferServer(connection);
                        connection.getTravelData().finishHandle();
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
                            if (!connection.getTravelData().isTraveling()) {
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
