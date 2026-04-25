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

    private final String serverAddress;
    private final int serverPort;
    private final int proxyPort;

    public NetworkThread(NetworkManager _netManager, String serverAddress, int serverPort, int proxyPort) {
        this._netManager = _netManager;

        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.proxyPort = proxyPort;
    }

    public void run() {
        try {
            ServerSocketChannel proxySocket = ServerSocketChannel.open();
            proxySocket.configureBlocking(false);
            proxySocket.bind(new InetSocketAddress(this.proxyPort));

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

                            LCEConnection connection = new LCEConnection(clientChannel);
                            if (_netManager.connectToServer(connection, this.serverAddress, this.serverPort)) {
                                _netManager.handleIncomingConnection(connection);
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
