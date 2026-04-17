//
// Created by PeakB on 4/12/2026.
//
#pragma once
#include <atomic>
#include <memory>

#include "Networking/NetworkManager.h"
#include "Utils/Logger.h"

class Config;

class LegacyCord {
public:
    LegacyCord();

    //void tick();

    static std::shared_ptr<NetworkManager> getNetworkManager();
    static std::shared_ptr<Logger> getLogger();
    std::shared_ptr<Config> getConfig();

    bool isRunning() const;
private:
    static std::shared_ptr<NetworkManager> _networkManager;
    static std::shared_ptr<Logger> _logger;
    std::shared_ptr<Config> _config;
};
