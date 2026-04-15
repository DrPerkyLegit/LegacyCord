//
// Created by PeakB on 4/12/2026.
//
#include <memory>
#include <Networking/NetworkManager.h>

#include "ConnectionThread.h"
#include "Platform/WinSocketNetworking.h"
#include "Utils/Config.h"

NetworkManager::NetworkManager(LegacyCord* core) {
    std::string proxyAddress = core->getConfig()->getString("proxy-host", "127.0.0.1");
    unsigned short proxyPort = core->getConfig()->getInt("proxy-port", 25565);

    std::string hostAddress = core->getConfig()->getString("server-host", "127.0.0.1");
    unsigned short hostPort = core->getConfig()->getInt("server-port", 25564);


#if defined(_WIN64)
    LegacyCord::getLogger()->Info("Starting Networking Platform - WinSock");
    this->_platformNetworkingStub = std::make_shared<WinSockNetworking>(this, proxyAddress, proxyPort, hostAddress, hostPort);
#elif defined(__linux__)

#endif

    int connectionThreads = core->getConfig()->getInt("connection-threads", 4);
    if (connectionThreads > 16 || connectionThreads <= 0) connectionThreads = 16;

    LegacyCord::getLogger()->Info("Starting ", connectionThreads, " Networking Threads");
    {
        std::lock_guard<std::mutex> _guard(connectionMutex);
        this->_networkThreads.assign(connectionThreads, nullptr);

        for (int i = 0; i < connectionThreads; i++) {
            this->_networkThreads[i] = std::make_shared<ConnectionThread>(this);
        }
    }

}

void NetworkManager::handleIncomingConnection(std::shared_ptr<PlayerConnection> connection) {
    {
        std::lock_guard<std::mutex> _guard(connectionMutex);
        this->_connections.push_back(connection);
    }

    std::shared_ptr<ConnectionThread> bestThread;
    size_t lowestThreadCount = INT_MAX;
    {
        std::lock_guard<std::mutex> _guard(threadsMutex);
        for (std::shared_ptr<ConnectionThread> thread : this->_networkThreads) {
            if (thread->getConnectionsCount() >= lowestThreadCount) continue;

            bestThread = thread;
        }
    }
    bestThread->addConnection(connection);
}

void NetworkManager::handleClosingConnection(std::shared_ptr<PlayerConnection> connection) {
    //we need to send a close player packet on the server if something happens here, client / server (check error type)
    {
        std::lock_guard<std::mutex> _guard(connectionMutex);
        std::erase(this->_connections, connection);
    }
}

bool NetworkManager::isListening() {
    return this->_platformNetworkingStub->isListening();
}

std::shared_ptr<GenericNetworkingStub> NetworkManager::getNetworkingInstance() {
    return this->_platformNetworkingStub;
}
