//
// Created by PeakB on 4/13/2026.
//
#pragma once

#include "../PlayerConnection.h"
#include "GenericNetworkingStub.h"
#include "LegacyCord.h"
#include "Utils/Logger.h"

class WinSockNetworking : public GenericNetworkingStub {
public:
    WinSockNetworking(NetworkManager* networkManager, std::string proxyAddress, unsigned short proxyPort, std::string hostAddress, unsigned short hostPort) : GenericNetworkingStub(networkManager, proxyAddress, proxyPort, hostAddress, hostPort) {
        this->_threadListening = true;

        this->_assignedThread = std::thread(&WinSockNetworking::tick, this);
        this->_assignedThread.detach();
    }

    virtual void tickConnection(std::shared_ptr<PlayerConnection> connection) {
        char buffer[8192];
        socket_t clientSocket = connection->getClientSocket();
        socket_t serverSocket = connection->getServerSocket();
        {
            int size = recv(clientSocket, buffer, sizeof(buffer), 0);
            if (size > 0) {
                connection->getPacketParser()->feed(buffer, size, false);
                if (!connection->isAuthenticationPending()) {
                    send(serverSocket, buffer, size, 0);
                }
            } else if (size == 0) {
                LegacyCord::getLogger()->Info("[WinSockNetworking::tickConnection] ", "Client Socket Closed: ", clientSocket);
                connection->setConnectionError(ConnectionErrors::ClientClosed);
                return;
            } else {
                int err = WSAGetLastError();
                if (err != WSAEWOULDBLOCK) {
                    LegacyCord::getLogger()->Warn("[WinSockNetworking::tickConnection] ", "Client Socket Closed - StreamError: ", clientSocket);
                    connection->setConnectionError(ConnectionErrors::ClientStreamError);
                    return;
                }
            }
        }

        {
            int size = recv(serverSocket, buffer, sizeof(buffer), 0);
            if (size > 0) {
                connection->getPacketParser()->feed(buffer, size, true);
                if (!connection->isAuthenticationPending()) {
                    send(clientSocket, buffer, size, 0);
                }
            } else if (size == 0) {
                LegacyCord::getLogger()->Debug("[WinSockNetworking::tickConnection] ", "Server Socket Closed: ", serverSocket);
                connection->setConnectionError(ConnectionErrors::ServerClosed);
                return;
            } else {
                int err = WSAGetLastError();
                if (err != WSAEWOULDBLOCK) {
                    LegacyCord::getLogger()->Debug("[WinSockNetworking::tickConnection] ", "Server Socket Closed - StreamError: ", serverSocket);
                    connection->setConnectionError(ConnectionErrors::ServerStreamError);
                    return;
                }
            }
        }

        {
            std::vector<std::shared_ptr<LCEPacket>> _packets = connection->getPendingPackets();

            for (auto& packet : _packets) {
                int packetSize = packet->readInt(0x00);
                send(clientSocket, packet->getRawData(), (packetSize + LCEPacket::HEADERSIZE), 0);
            }
        }

        if (connection->hasPendingTransfer()) {
            this->connectToNewServer(connection);
        }
    }

    virtual void connectToNewServer(std::shared_ptr<PlayerConnection> connection) {
        shutdown(connection->getServerSocket(), SD_BOTH);

        SOCKET server = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
        sockaddr_in serverAddr{};
        serverAddr.sin_family = AF_INET;
        serverAddr.sin_port = htons(connection->getPendingPort());
        inet_pton(serverAddr.sin_family, connection->getPendingHost().c_str(), &serverAddr.sin_addr);

        if (connect(server, reinterpret_cast<sockaddr*>(&serverAddr), sizeof(serverAddr)) != 0) {
            LegacyCord::getLogger()->Error("[WinSocketNetworking::tick] Failed To Connect To Minecraft Server During Transfer (", connection->getPendingHost().c_str(), ":", connection->getPendingPort(), "): ", WSAGetLastError());
            shutdown(connection->getClientSocket(), SD_BOTH); //todo: send kick packet to client instead of just closing the socket
            return;
        }

        //already set these properties on the client
        u_long mode = 1;
        ioctlsocket(server, FIONBIO, &mode);

        int disableDelay = true;
        setsockopt(server, IPPROTO_TCP, TCP_NODELAY, (char *)&disableDelay, sizeof(int));

        connection->setNewServer((socket_t)server);

        this->sendConstructedPacket(connection->getServerSocket(), connection->getPreLoginPacket());//this is a C2S packet so its parsed dif
    }

    virtual void sendConstructedPacket(socket_t socket, std::shared_ptr<LCEPacket> _packet) {
        int packetSize = _packet->getSize();

        char* tempBuffer = new char[(packetSize + 1 + LCEPacket::HEADERSIZE)];

        tempBuffer[0] = static_cast<uint8_t>((packetSize >> 24) & 0xFF);
        tempBuffer[1] = static_cast<uint8_t>((packetSize >> 16) & 0xFF);
        tempBuffer[2] = static_cast<uint8_t>((packetSize >> 8) & 0xFF);
        tempBuffer[3] = static_cast<uint8_t>(packetSize & 0xFF);

        tempBuffer[4] = static_cast<uint8_t>(_packet->getId());

        memcpy(tempBuffer + 5, _packet->getRawData(), packetSize);

        send(socket, tempBuffer, (packetSize + LCEPacket::HEADERSIZE), 0);

        delete[] tempBuffer;
    }

    virtual void sendRawPacket(socket_t socket, std::shared_ptr<LCEPacket> _packet) {
        int packetSize = _packet->readInt(0x00);
        send(socket, _packet->getRawData(), (packetSize + LCEPacket::HEADERSIZE), 0);
    }

    void tick() {
        WSADATA wsa;
        if (WSAStartup(MAKEWORD(2, 2), &wsa) != 0) {
            LegacyCord::getLogger()->Error("[WinSocketNetworking::tick] WSAStartup Failed: ", WSAGetLastError());
            return;
        }

        SOCKET listener = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);

        sockaddr_in addr{};
        addr.sin_family = AF_INET;
        addr.sin_port = htons(this->_proxyPort);
        inet_pton(addr.sin_family, this->_proxyAddress.c_str(), &addr.sin_addr);

        if (bind(listener, reinterpret_cast<sockaddr*>(&addr), sizeof(addr)) != 0) {
            LegacyCord::getLogger()->Error("[WinSocketNetworking::tick] Bind Failed (", this->_proxyAddress.c_str(), ":", this->_proxyPort, "): ", WSAGetLastError());
            return;
        }

        if (listen(listener, SOMAXCONN) != 0) {
            LegacyCord::getLogger()->Error("[WinSocketNetworking::tick] Listen Failed: %", WSAGetLastError());
            return;
        }

        while (this->_networkManager->isListening()) {
            SOCKET connectingClient = accept(listener, nullptr, nullptr);

            SOCKET server = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
            sockaddr_in serverAddr{};
            serverAddr.sin_family = AF_INET;
            serverAddr.sin_port = htons(this->_hostPort);
            inet_pton(serverAddr.sin_family, this->_hostAddress.c_str(), &serverAddr.sin_addr);

            if (connect(server, reinterpret_cast<sockaddr*>(&serverAddr), sizeof(serverAddr)) != 0) {
                LegacyCord::getLogger()->Error("[WinSocketNetworking::tick] Failed To Connect To Minecraft Server (", this->_hostAddress.c_str(), ":", this->_hostPort, "): ", WSAGetLastError());
                shutdown(connectingClient, SD_BOTH); //todo: send kick packet to client instead of just closing the socket
                continue;
            }

            u_long mode = 1;
            ioctlsocket(connectingClient, FIONBIO, &mode);
            ioctlsocket(server, FIONBIO, &mode);

            int disableDelay = true;
            setsockopt(connectingClient, IPPROTO_TCP, TCP_NODELAY, (char *)&disableDelay, sizeof(int));
            setsockopt(server, IPPROTO_TCP, TCP_NODELAY, (char *)&disableDelay, sizeof(int));

            std::shared_ptr<PlayerConnection> clientConnection = std::make_shared<PlayerConnection>(static_cast<socket_t>(connectingClient), static_cast<socket_t>(server));

            LegacyCord::getLogger()->Info("[WinSocketNetworking::tick] ", "Incoming Client Connection: ", clientConnection->getClientSocket());

            this->_networkManager->handleIncomingConnection(clientConnection);
        }

        this->_threadListening = false;
        WSACleanup();
    }
};
