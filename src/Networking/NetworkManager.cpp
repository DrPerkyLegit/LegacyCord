//
// Created by PeakB on 4/12/2026.
//
#include <memory>
#include <Networking/NetworkManager.h>

#include "ConnectionThread.h"
#include "Utils/Config.h"
#include "LegacyCord.h"

#if defined(_WIN64)
#include "Platform/WinSocketNetworking.h"
#elif defined(__linux__)
#include "Platform/POSIXNetworking.h"
#endif

NetworkManager::NetworkManager(LegacyCord* core) {
    std::string proxyAddress = core->getConfig()->getString("proxy-host", "0.0.0.0");
    unsigned short proxyPort = core->getConfig()->getInt("proxy-port", 25565);

    std::string hostAddress = core->getConfig()->getString("server-host", "127.0.0.1");
    unsigned short hostPort = core->getConfig()->getInt("server-port", 25564);


#if defined(_WIN64)
    LegacyCord::getLogger()->Info("Starting Networking Platform - WinSock");
    this->_platformNetworkingStub = std::make_shared<WinSockNetworking>(this, proxyAddress, proxyPort, hostAddress, hostPort);
#elif defined(__linux__)
    LegacyCord::getLogger()->Info("Starting Networking Platform - POSIX");
    this->_platformNetworkingStub = std::make_shared<POSIXNetworking>(this, proxyAddress, proxyPort, hostAddress, hostPort);
    //posix based threads start as soon we detach them, that makes _platformNetworkingStub null because the constructor hasnt completed
    //so as a workaround to that issue we just start the thread after we know its valid
    //the issue this caused was inside the thread calling NetworkManager::isListening() while _platformNetworkingStub is null
    dynamic_cast<POSIXNetworking *>(this->_platformNetworkingStub.get())->start();
#endif

    LegacyCord::getLogger()->Info("Creating Networking Pipelines");



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

//this is called from connection threads so we need to make sure anything ran here is quick
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

//we need to send a close player packet on the server if something happens here, client / server (check error type)
//this is called from connection threads so we need to make sure anything ran here is quick
void NetworkManager::handleClosingConnection(std::shared_ptr<PlayerConnection> connection) {
    GenericConnectionError error = PlayerConnection::genericError(connection->getConnectionError());

    //todo: abstract closing code to the platform networking files
    if (error == GenericConnectionError::Server) {
        //server error, close client
#if defined(_WIN32)
        shutdown(connection->getClientSocket(), SD_BOTH);
#else
        shutdown(connection->getClientSocket(), SHUT_RDWR);
        close(connection->getClientSocket());
#endif
    }

    if (error == GenericConnectionError::Client) {
        //client error, close server
#if defined(_WIN32)
        shutdown(connection->getServerSocket(), SD_BOTH);
#else
        shutdown(connection->getServerSocket(), SHUT_RDWR);
        close(connection->getServerSocket());
#endif
    }

    {
        std::lock_guard<std::mutex> _guard(connectionMutex);
        std::erase(this->_connections, connection);
    }
}

//we use raw pointer here cause packet parser cant get the shared_ptr from where its made
//packet data is kinda broken, theres some kinda issue with packet parsing so its slightly broken, should be fine for most packets
bool NetworkManager::handlePlayerPacket(PlayerConnection *connection, std::shared_ptr<LCEPacket> packet, bool fromServer) {
    LegacyCord::getLogger()->Debug((fromServer ? "[S2C]"  : "[C2S]"), " ID:", static_cast<int>(packet->getId()), " Size: ", packet->getSize());
    bool pendingAuth = connection->isAuthenticationPending();
    const uint8_t packetId = packet->getId();

    if (pendingAuth && fromServer) {
        if (packetId == 1) {
            LegacyCord::getLogger()->Debug("Server Sent Correct Packet Flow");

            //_platformNetworkingStub->sendRawPacket();

            connection->setTransferFinished();
            return false;
        } else if (packetId == 2) {
            _platformNetworkingStub->sendConstructedPacket(connection->getServerSocket(), connection->getLoginPacket());
            return false;
        }
    } else if (!pendingAuth && !fromServer) {
        if (packetId == 1) {
            connection->cacheLoginPacket(packet);
            return true;

        } else if (packetId == 2) {
            connection->cachePreLoginPacket(packet);
            return true;
        }
    }

    if (!fromServer && packet->getId() == 3) { //chat packet
        int messageType = packet->readShort(0x00);
        if (messageType == 0) { //custom message
            LegacyCord::getLogger()->Debug("Player Sent Custom Message");

            connection->sendToServer("127.0.0.1", 25566);
        }
    }
    if (packet->getId() == 9) { //respawn packet

    }
    return true;
}

bool NetworkManager::isListening() {
    return this->_platformNetworkingStub->isListening();
}

std::shared_ptr<GenericNetworkingStub> NetworkManager::getNetworkingInstance() {
    return this->_platformNetworkingStub;
}
