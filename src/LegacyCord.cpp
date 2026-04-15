//
// Created by PeakB on 4/12/2026.
//
#include <LegacyCord.h>
#include <Utils/Logger.h>
#include <Utils/Config.h>

#include <chrono>
#include <thread>

std::shared_ptr<Logger> LegacyCord::_logger = std::make_shared<Logger>(nullptr);

LegacyCord::LegacyCord() {
    this->_config = std::make_shared<Config>();
    getLogger()->Info("Loading LegacyCord");

    this->_config->load("server.properties");
    getLogger()->setDebugOutput(this->_config->getBool("debug-config", false));

    this->_networkManager = std::make_shared<NetworkManager>(this);
}

bool LegacyCord::isRunning() const {
    return this->_networkManager->isListening();
}

//void LegacyCord::tick() { }

std::shared_ptr<Logger> LegacyCord::getLogger() {
    return _logger;
}

std::shared_ptr<Config> LegacyCord::getConfig() {
    return _config;
}

int main() {
    LegacyCord* legacyCord = new LegacyCord();

    while (legacyCord->isRunning()) {
        //legacyCord->tick();
        std::this_thread::sleep_for(std::chrono::milliseconds(1));
    }

    delete legacyCord;
    return 0;
}
