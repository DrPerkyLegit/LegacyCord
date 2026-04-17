//
// Created by PeakB on 4/13/2026.
//
#pragma once
#include <atomic>
#include <vector>

class ConnectionThread;
class PlayerConnection;
class GenericNetworkingStub;
class LegacyCord;
class LCEPacket;

class NetworkManager {
public:
    NetworkManager(LegacyCord* cord);

    void handleIncomingConnection(std::shared_ptr<PlayerConnection> connection);
    void handleClosingConnection(std::shared_ptr<PlayerConnection> connection);
    bool handlePlayerPacket(PlayerConnection* connection, std::shared_ptr<LCEPacket> packet, bool fromServer);

    bool isListening();

    std::shared_ptr<GenericNetworkingStub> getNetworkingInstance();
private:
    std::shared_ptr<GenericNetworkingStub> _platformNetworkingStub;
    std::atomic<bool> _isRunning = true;

    std::mutex connectionMutex;
    std::vector<std::shared_ptr<PlayerConnection>> _connections;
    std::mutex threadsMutex;
    std::vector<std::shared_ptr<ConnectionThread>> _networkThreads;
};