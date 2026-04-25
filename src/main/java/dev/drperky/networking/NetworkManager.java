package dev.drperky.networking;

import dev.drperky.networking.datatypes.LCEConnection;
import dev.drperky.networking.datatypes.LCEPacket;
import dev.drperky.networking.pipelines.NetworkPipeline;
import dev.drperky.networking.pipelines.NetworkPipelineContext;
import dev.drperky.networking.pipelines.NetworkPipelineType;
import dev.drperky.networking.pipelines.both.StreamDecoderPipeline;
import dev.drperky.networking.pipelines.both.StreamEncoderPipeline;
import dev.drperky.networking.pipelines.clientbound.ProxyMessagePacketPipeline;
import dev.drperky.networking.threads.ConnectionThread;
import dev.drperky.networking.threads.NetworkThread;
import dev.drperky.utils.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;

public class NetworkManager {
    NetworkThread _networkThread;
    final Set<ConnectionThread> _connectionThreads = new HashSet<>();

    final Map<NetworkPipelineType, List<NetworkPipeline>> _registeredPipelines = new HashMap<>();

    public NetworkManager(String serverAddress, int serverPort, int proxyPort) {
        //keep decoders at the top and the encoders at the bottom for pipelines
        {
            List<NetworkPipeline> pipelineList = new ArrayList<>();
            pipelineList.add(new StreamDecoderPipeline());
            pipelineList.add(new StreamEncoderPipeline());

            _registeredPipelines.put(NetworkPipelineType.SERVERBOUND, pipelineList);
        }

        {
            List<NetworkPipeline> pipelineList = new ArrayList<>();
            pipelineList.add(new StreamDecoderPipeline());
            pipelineList.add(new ProxyMessagePacketPipeline());
            pipelineList.add(new StreamEncoderPipeline());

            _registeredPipelines.put(NetworkPipelineType.CLIENTBOUND, pipelineList);
        }

        for (int i = 0; i < 4; i++) {
            ConnectionThread newThread = new ConnectionThread(this);
            newThread.start();

            _connectionThreads.add(newThread);
        }

        this._networkThread = new NetworkThread(this, serverAddress, serverPort, proxyPort);
        this._networkThread.start();
    };

    public void handleIncomingConnection(LCEConnection connection) {
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

    public void handleClosingConnection(LCEConnection connection) {
        try {
            //ConnectionError error = connection.getPendingError();

            //todo: handle errors, for now we just close sockets if possible
            if (connection.getClientChannel() != null) {

                connection.getClientChannel().close();
            }
            if (connection.getServerChannel() != null) {
                connection.getServerChannel().close();
            }
        } catch(Exception e) {}
    }

    public void handleIncomingStream(LCEConnection connection, ByteBuffer buffer, boolean serverbound) {
        try {
            List<NetworkPipeline> pipelines = _registeredPipelines.get((serverbound ? NetworkPipelineType.SERVERBOUND : NetworkPipelineType.CLIENTBOUND));
            NetworkPipelineContext ctx = new NetworkPipelineContext(connection, buffer, serverbound);

            for (NetworkPipeline pipeline : pipelines) {
                if (!pipeline.pipe(ctx)) {
                    //todo: add debug log for pipeline closing, check kick status and handle it
                    return;
                }
            }

        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public boolean transferServer(LCEConnection connection) {
        try {
            if (connection.getServerChannel() != null) connection.getServerChannel().close();

            boolean didConnect = connectToServer(connection, connection.getTravelData().getQueuedTravelHost(), connection.getTravelData().getQueuedTravelPort());
            if (didConnect) {
                connection.getServerChannel().write(connection.getTravelData().getCachedPacket_PreLogin().CompressPacket());
            }

            return didConnect;
        } catch(Exception e) {
            Logger.Warn("Client is unable to connect to transfer server");
            handleClosingConnection(connection);
        }

        return false;
    }

    public boolean connectToServer(LCEConnection connection, String serverAddress, int serverPort) {
        try {
            SocketChannel serverChannel = SocketChannel.open();
            serverChannel.connect(new InetSocketAddress(serverAddress, serverPort));

            serverChannel.configureBlocking(false);
            serverChannel.socket().setTcpNoDelay(true);

            connection.setConnectedServer(serverChannel);
        } catch (IOException e) {
            Logger.Warn("Client is unable to connect to server (", serverAddress, ":", String.valueOf(serverPort), ")");
            handleClosingConnection(connection);
            return false;
        }

        return true;
    }

    public boolean isTicking() {
        return this._networkThread.isAlive();
    }
}
