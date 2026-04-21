package dev.drperky.networking.threads;

import dev.drperky.networking.datatypes.LCEConnection;
import dev.drperky.networking.NetworkManager;
import dev.drperky.networking.datatypes.ServerTravelData;
import dev.drperky.utils.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NetworkThread extends Thread {
    private final NetworkManager _netManager;

    public NetworkThread(NetworkManager _netManager) {
        this._netManager = _netManager;
    }

    public static void transferServer(LCEConnection connection) {
        try {
            if (connection.getServerChannel() != null) connection.getServerChannel().close();
            ServerTravelData travelData = connection.getTravelData();

            SocketChannel serverChannel = SocketChannel.open();
            serverChannel.connect(new InetSocketAddress(travelData.getQueuedTravelHost(), travelData.getQueuedTravelPort()));

            serverChannel.configureBlocking(false);
            serverChannel.socket().setTcpNoDelay(true);

            connection.setConnectedServer(serverChannel);
            connection.getServerChannel().write(travelData.getCachedPacket_PreLogin().CompressPacket());
        } catch(Exception e) { }
    }

    public void run() {
        try {
            ServerSocketChannel proxySocket = ServerSocketChannel.open();
            proxySocket.configureBlocking(false);
            proxySocket.bind(new InetSocketAddress(25565));

            Selector selector = Selector.open();
            proxySocket.register(selector, SelectionKey.OP_ACCEPT);

            while (!Thread.currentThread().isInterrupted()) {
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (key.isAcceptable()) {
                        SocketChannel clientChannel = ((ServerSocketChannel)key.channel()).accept();
                        if (clientChannel != null) {
                            clientChannel.configureBlocking(false);
                            clientChannel.socket().setTcpNoDelay(true);

                            InetSocketAddress serverAddress = new InetSocketAddress("127.0.0.1", 25564);
                            try {
                                SocketChannel serverChannel = SocketChannel.open();
                                serverChannel.connect(serverAddress);

                                serverChannel.configureBlocking(false);
                                serverChannel.socket().setTcpNoDelay(true);

                                LCEConnection LCEConnection = new LCEConnection(clientChannel);
                                LCEConnection.setConnectedServer(serverChannel);

                                _netManager.handleIncomingConnection(LCEConnection);
                            } catch (IOException e) {
                                Logger.Warn("Client is unable to connect to server (", serverAddress.toString(), ")");
                                //todo: send client kick message
                            }
                        }
                    }
                }

                sleep(1);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
