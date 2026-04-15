//
// Created by PeakB on 4/12/2026.
//
#pragma once

#include <iostream>
#include <mutex>

class Logger {
public:
    Logger(const char* prefix) : _prefix(prefix) { }

    template<typename... Args>
    void Debug(Args&&... args) {
        if (!enabledDebug) return;
        std::lock_guard<std::mutex> guard(coutMutex);
        if (_prefix != nullptr) std::cout << "[" << _prefix << "] ";
        std::cout << "[DEBUG] ";
        (std::cout << ... << args) << std::endl;
    }

    template<typename... Args>
    void Info(Args&&... args) {
        std::lock_guard<std::mutex> guard(coutMutex);
        if (_prefix != nullptr) std::cout << "[" << _prefix << "] ";
        std::cout << "[INFO] ";
        (std::cout << ... << args) << std::endl;
    }

    template<typename... Args>
    void Warn(Args&&... args) {
        std::lock_guard<std::mutex> guard(coutMutex);
        if (_prefix != nullptr) std::cout << "[" << _prefix << "] ";
        std::cout << "[WARNING] ";
        (std::cout << ... << args) << std::endl;
    }

    template<typename... Args>
    void Error(Args&&... args) {
        std::lock_guard<std::mutex> guard(coutMutex);
        if (_prefix != nullptr) std::cout << "[" << _prefix << "] ";
        std::cout << "[ERROR] ";
        (std::cout << ... << args) << std::endl;
    }

    void setDebugOutput(bool value) {
        this->enabledDebug = value;
    }
private:
    const char* _prefix;
    std::mutex coutMutex;

    bool enabledDebug = false;
};
