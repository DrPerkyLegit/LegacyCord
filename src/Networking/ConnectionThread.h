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
        std::vector<std::shared_ptr<PlayerConnection>> staleConnections;

        while (true) {
            std::vector<std::shared_ptr<PlayerConnection>> localConnections; //connections snapshot
            {
                std::lock_guard<std::mutex> lock(connectionMutex);

                if (!staleConnections.empty()) {
                    for (auto& c : staleConnections) {
                        std::erase(connections, c);
                    }
                }

                localConnections = connections;
            }

            if (!staleConnections.empty()) {
                for (auto& c : staleConnections) {
                    networkManager->handleClosingConnection(c);
                }
                staleConnections.clear();
            }

            for (auto& c : localConnections) {
                networkManager->getNetworkingInstance()->tickConnection(c);

                if (c->getConnectionError() == ConnectionErrors::None) continue;

                staleConnections.push_back(c);
            }

            std::this_thread::sleep_for(std::chrono::milliseconds(1));
        }
    }
};
