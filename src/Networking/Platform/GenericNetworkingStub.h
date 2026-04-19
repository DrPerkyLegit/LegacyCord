//
// Created by PeakB on 4/13/2026.
//
#pragma once
#include <thread>
#include <utility>

class NetworkManager;

class GenericNetworkingStub {
public:
    GenericNetworkingStub(NetworkManager* networkManager, std::string proxyAddress, unsigned short proxyPort, std::string hostAddress, unsigned short hostPort) : _networkManager(networkManager), _proxyAddress(proxyAddress), _proxyPort(proxyPort), _hostAddress(hostAddress), _hostPort(hostPort) {};
    virtual ~GenericNetworkingStub() = default;

    virtual void tickConnection(std::shared_ptr<PlayerConnection> connection) = 0;
    virtual void sendConstructedPacket(socket_t socket, std::shared_ptr<LCEPacket> _packet) = 0;
    virtual void sendRawPacket(socket_t socket, std::shared_ptr<LCEPacket> _packet) = 0;

    bool isListening() const {
        return this->_threadListening;
    };
protected:
    std::thread _assignedThread;
    NetworkManager* _networkManager;

    std::atomic<bool> _threadListening = false;
    std::string _proxyAddress = "";
    unsigned short _proxyPort = 0;

    std::string _hostAddress = "";
    unsigned short _hostPort = 0;
};
