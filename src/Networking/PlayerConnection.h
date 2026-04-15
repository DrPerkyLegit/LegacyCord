//
// Created by PeakB on 4/13/2026.
//
#pragma once
#include "Packets/PacketParser.h"

#if defined(_WIN32)
#include <winsock2.h>
#include <ws2tcpip.h>

typedef SOCKET socket_t;

#else

#endif

enum class ConnectionErrors {
    None,
    ServerClosed,
    ClientClosed,

    ServerStreamError,
    ClientStreamError
};

class PlayerConnection {
public:
    PlayerConnection(socket_t client, socket_t server) : clientConnection(client), serverConnection(server), _isPending(true), _packetReader(std::make_shared<PacketParser>()) {}

    ConnectionErrors getConnectionError() const { return this->_currentError; }
    void setConnectionError(const ConnectionErrors error) { this->_currentError = error; }

    void flushConnectionError() { this->_currentError = ConnectionErrors::None; }

    socket_t getClientSocket() const { return this->clientConnection; }
    socket_t getServerSocket() const { return this->serverConnection; }

    std::shared_ptr<PacketParser> getPacketParser() { return this->_packetReader; }
private:
    std::atomic<bool> _isPending;

    std::shared_ptr<PacketParser> _packetReader;

    ConnectionErrors _currentError = ConnectionErrors::None;
    socket_t clientConnection = (socket_t)(~0);
    socket_t serverConnection = (socket_t)(~0);
};