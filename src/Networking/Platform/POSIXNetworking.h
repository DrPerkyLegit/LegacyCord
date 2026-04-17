//
// Created by PeakB on 4/13/2026.
//
#pragma once

#include "../PlayerConnection.h"
#include "GenericNetworkingStub.h"
#include "LegacyCord.h"
#include "Utils/Logger.h"

class POSIXNetworking : public GenericNetworkingStub {
public:
    POSIXNetworking(NetworkManager* networkManager, std::string proxyAddress, unsigned short proxyPort, std::string hostAddress, unsigned short hostPort) : GenericNetworkingStub(networkManager, proxyAddress, proxyPort, hostAddress, hostPort) {

    }

    void start() {
        this->_threadListening = true;

        this->_assignedThread = std::thread(&POSIXNetworking::tick, this);
        this->_assignedThread.detach();
    }

    virtual void tickConnection(std::shared_ptr<PlayerConnection> connection) {
        char buffer[4096];
        socket_t clientSocket = connection->getClientSocket();
        socket_t serverSocket = connection->getServerSocket();
        {
            ssize_t size = recv(clientSocket, buffer, sizeof(buffer), 0);
            if (size > 0) {
                connection->getPacketParser()->feed(buffer, size, false);
                send(serverSocket, buffer, size, 0);
            } else if (size == 0) {
                LegacyCord::getLogger()->Info("[POSIXNetworking::tickConnection] ", "Client Socket Closed: ", clientSocket);
                connection->setConnectionError(ConnectionErrors::ClientClosed);
                return;
            } else {
                if (errno != EWOULDBLOCK && errno != EAGAIN) {
                    LegacyCord::getLogger()->Warn("[POSIXNetworking::tickConnection] ", "Client Socket Closed - StreamError: ", clientSocket);
                    connection->setConnectionError(ConnectionErrors::ClientStreamError);
                    return;
                }
            }
        }

        {
            ssize_t size = recv(serverSocket, buffer, sizeof(buffer), 0);
            if (size > 0) {
                connection->getPacketParser()->feed(buffer, size, true);
                send(clientSocket, buffer, size, 0);
            } else if (size == 0) {
                LegacyCord::getLogger()->Debug("[POSIXNetworking::tickConnection] ", "Server Socket Closed: ", serverSocket);
                connection->setConnectionError(ConnectionErrors::ServerClosed);
                return;
            } else {
                if (errno != EWOULDBLOCK && errno != EAGAIN) {
                    LegacyCord::getLogger()->Debug("[POSIXNetworking::tickConnection] ", "Server Socket Closed - StreamError: ", serverSocket);
                    connection->setConnectionError(ConnectionErrors::ServerStreamError);
                    return;
                }
            }
        }
    }

    void tick() {
        int listener = socket(AF_INET, SOCK_STREAM, 0);
        if (listener == -1) {
            LegacyCord::getLogger()->Debug("[POSIXNetworking::tick] ", "Failed To Open Socket");
            return;
        }

        sockaddr_in addr{};
        addr.sin_family = AF_INET;
        addr.sin_port = htons(this->_proxyPort);
        inet_pton(addr.sin_family, this->_proxyAddress.c_str(), &addr.sin_addr);

        if (bind(listener, reinterpret_cast<sockaddr*>(&addr), sizeof(addr)) != 0) {
            LegacyCord::getLogger()->Error("[POSIXNetworking::tick] Bind Failed (", this->_proxyAddress.c_str(), ":", this->_proxyPort, "): ", strerror(errno));
            return;
        }

        if (listen(listener, SOMAXCONN) != 0) {
            LegacyCord::getLogger()->Error("[POSIXNetworking::tick] Listen Failed: ", strerror(errno));
            return;
        }

        while (this->_networkManager->isListening()) {
            int connectingClient = accept(listener, nullptr, nullptr);
            if (connectingClient < 0) continue;

            int serverSocket = socket(AF_INET, SOCK_STREAM, 0);

            sockaddr_in serverAddr{};
            serverAddr.sin_family = AF_INET;
            serverAddr.sin_port = htons(this->_hostPort);
            inet_pton(serverAddr.sin_family, this->_hostAddress.c_str(), &serverAddr.sin_addr);

            if (connect(serverSocket, reinterpret_cast<sockaddr*>(&serverAddr), sizeof(serverAddr)) != 0) {
                LegacyCord::getLogger()->Error("[POSIXNetworking::tick] Failed To Connect To Minecraft Server (", this->_hostAddress.c_str(), ":", this->_hostPort, "): ", strerror(errno));
                shutdown(connectingClient, SHUT_RDWR);
                close(connectingClient);
                continue;
            }

            fcntl(connectingClient, F_SETFL, O_NONBLOCK);
            fcntl(serverSocket, F_SETFL, O_NONBLOCK);

            int disableDelay = 1;
            setsockopt(connectingClient, IPPROTO_TCP, TCP_NODELAY, &disableDelay, sizeof(int));
            setsockopt(serverSocket, IPPROTO_TCP, TCP_NODELAY, &disableDelay, sizeof(int));

            std::shared_ptr<PlayerConnection> clientConnection = std::make_shared<PlayerConnection>(static_cast<socket_t>(connectingClient), static_cast<socket_t>(serverSocket));

            LegacyCord::getLogger()->Info("[POSIXNetworking::tick] ", "Incoming Client Connection: ", clientConnection->getClientSocket());

            this->_networkManager->handleIncomingConnection(clientConnection);
        }

        this->_threadListening = false;
        close(listener);
    }
};
