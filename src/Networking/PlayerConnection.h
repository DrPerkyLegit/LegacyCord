//
// Created by PeakB on 4/13/2026.
//
#pragma once
#include "Packets/PacketParser.h"
#include "../PlatformVariables.h"

enum class GenericConnectionError {
    None,
    Server,
    Client
};

enum class ConnectionErrors {
    None,
    ServerClosed,
    ServerStreamError,

    ServerError_MAX,

    ClientClosed,
    ClientStreamError,

    ClientError_MAX,
};

class PlayerConnection {
public:
    PlayerConnection(socket_t client, socket_t server) : clientConnection(client), serverConnection(server), _isPending(true), _packetReader(std::make_shared<PacketParser>(this)) {}

    ConnectionErrors getConnectionError() const { return this->_currentError; }
    void setConnectionError(const ConnectionErrors error) { this->_currentError = error; }

    void flushConnectionError() { this->_currentError = ConnectionErrors::None; }

    socket_t getClientSocket() const { return this->clientConnection; }
    socket_t getServerSocket() const { return this->serverConnection; }

    static GenericConnectionError genericError(ConnectionErrors error) {
        if (error > ConnectionErrors::None && error < ConnectionErrors::ServerError_MAX) return GenericConnectionError::Server;
        if (error > ConnectionErrors::ServerError_MAX && error < ConnectionErrors::ClientError_MAX) return GenericConnectionError::Client;

        return GenericConnectionError::None;
    }

    std::shared_ptr<PacketParser> getPacketParser() { return this->_packetReader; }
private:
    std::atomic<bool> _isPending;

    std::shared_ptr<PacketParser> _packetReader;

    ConnectionErrors _currentError = ConnectionErrors::None;
    socket_t clientConnection = (socket_t)(~0);
    socket_t serverConnection = (socket_t)(~0);
};