package dev.drperky.networking;

import dev.drperky.networking.datatypes.ConnectionError;
import dev.drperky.networking.datatypes.PlayerConnection;
import dev.drperky.networking.threads.ConnectionThread;
import dev.drperky.networking.threads.NetworkThread;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NetworkManager {
    NetworkThread _networkThread;
    final Set<ConnectionThread> _connectionThreads = new HashSet<>();

    public NetworkManager() {
        for (int i = 0; i < 4; i++) {
            ConnectionThread newThread = new ConnectionThread(this);
            newThread.start();

            _connectionThreads.add(newThread);
        }

        this._networkThread = new NetworkThread(this);
        this._networkThread.start();
    };

    public void handleIncomingConnection(PlayerConnection connection) {
        ConnectionThread bestThread = null;

        synchronized (_connectionThreads) {
            int lowestCount = Integer.MAX_VALUE;
            for (ConnectionThread thread : _connectionThreads) {
                int connectionCount = thread.getAssignedClientCount();
                if (connectionCount >= lowestCount) continue;

                lowestCount = connectionCount;
                bestThread = thread;
            }
        }

        if (bestThread != null) {
            bestThread.assignNewConnection(connection);
        }
    }

    public void handleClosingConnection(PlayerConnection connection) {
        try {
            //ConnectionError error = connection.getPendingError();

            //todo: handle errors, for now we just close sockets if possible
            if (connection.getClientChannel() != null) connection.getClientChannel().close();
            if (connection.getServerChannel() != null) connection.getServerChannel().close();
        } catch(Exception e) {}
    }

    public void handleIncomingStream(PlayerConnection connection, ByteBuffer buffer, boolean serverbound) {

        /*for (NetworkPipeline pipeline : _registeredPipelines) {
            if (!pipeline.feed(connection, buffer, serverbound)) {
                //todo: add debug log for pipeline closing a packet
                return; //cancel pipeline if its canceled
            }
        }*/
        try {
            (serverbound ? connection.getClientReader() : connection.getServerReader()).feed(buffer, serverbound);
            if (!connection.isTraveling()) {
                (serverbound ? connection.getServerChannel() : connection.getClientChannel()).write(buffer);
            }
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isTicking() {
        return this._networkThread.isAlive();
    }
}
