//
// Created by PeakB on 4/13/2026.
//
#pragma once
#include <atomic>
#include <memory>
#include <thread>
#include <vector>

#include "PlayerConnection.h"
#include "Platform/GenericNetworkingStub.h"

class ConnectionThread {
public:
    ConnectionThread(NetworkManager* networkManager) {
        this->assignedThread = std::thread(&ConnectionThread::tick, this, networkManager);
        this->assignedThread.detach();
    }

    void addConnection(const std::shared_ptr<PlayerConnection> &connection) {
        std::lock_guard<std::mutex> _guard(connectionMutex);
        this->connections.push_back(connection);
    }

    size_t getConnectionsCount() const { return this->connections.size(); }

private:
    std::vector<std::shared_ptr<PlayerConnection>> connections;
    std::thread assignedThread;
    std::mutex connectionMutex;

    void tick(NetworkManager* networkManager) {

        while (true) {
            std::vector<std::shared_ptr<PlayerConnection>> localConnections; //snapshot
            {
                std::lock_guard<std::mutex> lock(connectionMutex);
                localConnections = connections;
            }

            std::vector<std::shared_ptr<PlayerConnection>> staleConnections;

            for (auto& c : localConnections) {
                networkManager->getNetworkingInstance()->tickConnection(c);

                if (c->getConnectionError() == ConnectionErrors::None) continue;

                staleConnections.push_back(c);
            }

            if (staleConnections.size() > 0) {
                {
                    std::lock_guard<std::mutex> lock(connectionMutex);

                    for (auto& c : staleConnections) {
                        std::erase(connections, c);
                    }
                }

                for (auto& c : staleConnections) {
                    networkManager->handleClosingConnection(c);
                }

                staleConnections.clear();
            }

            std::this_thread::sleep_for(std::chrono::milliseconds(1));
        }
    }
};
