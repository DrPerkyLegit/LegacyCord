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
    PlayerConnection(socket_t client, socket_t server) : clientConnection(client), serverConnection(server), _authenticationPending(false), _transferPending(false), _packetReader(std::make_shared<PacketParser>(this)) {
        this->_preLoginPacket = nullptr;
        this->_loginPacket = nullptr;

        this->_newHost = "";
        this->_newPort = 0;
    }

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

    void cachePreLoginPacket(std::shared_ptr<LCEPacket>& _packet) {
        this->_preLoginPacket = _packet;
    }

    std::shared_ptr<LCEPacket> getPreLoginPacket() {
        return this->_preLoginPacket;
    }

    void cacheLoginPacket(std::shared_ptr<LCEPacket>& _packet) {
        this->_loginPacket = _packet;
    }

    std::shared_ptr<LCEPacket> getLoginPacket() {
        return this->_loginPacket;
    }

    void setNewServer(socket_t socket) {
        std::lock_guard<std::mutex> _guard(_variableMutex);
        this->_transferPending = false;
        this->_authenticationPending = true;
        this->_newHost = "";
        this->_newPort = 0;

        this->serverConnection = socket;
        this->getPacketParser()->resetServerBuffer();
    }

    void sendToServer(std::string host, unsigned short port) {
        std::lock_guard<std::mutex> _guard(_variableMutex);

        this->_transferPending = true;
        this->_newHost = host;
        this->_newPort = port;
    }

    bool hasPendingTransfer() {
        std::lock_guard<std::mutex> _guard(_variableMutex);
        return this->_transferPending;
    }

    void setTransferFinished() {
        std::lock_guard<std::mutex> _guard(_variableMutex);
        this->_authenticationPending = false;
    }

    bool isAuthenticationPending() {
        std::lock_guard<std::mutex> _guard(_variableMutex);
        return this->_authenticationPending;
    }

    std::string getPendingHost() {
        return this->_newHost;
    }

    unsigned short getPendingPort() const {
        return this->_newPort;
    }

    std::vector<std::shared_ptr<LCEPacket>> getPendingPackets() {
        std::lock_guard<std::mutex> _guard(_variableMutex);
        return _pendingPackets; //we copy the array so mutex doesnt need to stick around for thread safety, basically a snapshot
    }

    void queueCustomPacket(std::shared_ptr<LCEPacket>& _packet) {
        std::lock_guard<std::mutex> _guard(_variableMutex);
        _pendingPackets.emplace_back(_packet);
    }

    std::shared_ptr<PacketParser> getPacketParser() { return this->_packetReader; }
private:
    std::mutex _variableMutex; //used where i think its needed

    std::atomic<bool> _authenticationPending;

    std::atomic<bool> _transferPending;
    std::string _newHost;
    unsigned short _newPort;

    std::shared_ptr<PacketParser> _packetReader;

    std::vector<std::shared_ptr<LCEPacket>> _pendingPackets;

    std::shared_ptr<LCEPacket> _preLoginPacket;
    std::shared_ptr<LCEPacket> _loginPacket;

    ConnectionErrors _currentError = ConnectionErrors::None;
    socket_t clientConnection = (socket_t)(~0);
    socket_t serverConnection = (socket_t)(~0);
};