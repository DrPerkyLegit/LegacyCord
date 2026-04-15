//
// Created by PeakB on 4/13/2026.
//
#pragma once
#include <string>
#include <unordered_map>
#include <fstream>
#include <sstream>
#include <algorithm>
#include <filesystem>

#ifdef _WIN32
#include <windows.h>
#else
#include <unistd.h>
#include <limits.h>
#endif

static std::vector<std::string> defaultConfig = {
    "version=1",
    "proxy-host=127.0.0.1",
    "proxy-port=25565",
    "server-host=127.0.0.1",
    "server-port=25565",
    "connection-threads=4",

    "debug-logs=false"
};

inline std::filesystem::path GetExecutablePath() {
    char buffer[PATH_MAX];
#ifdef _WIN32
    GetModuleFileNameA(nullptr, buffer, PATH_MAX);
    return { buffer };

#else
    ssize_t len = readlink("/proc/self/exe", buffer, sizeof(buffer) - 1);
    buffer[len] = '\0';
    return  { buffer };
#endif
}

class Config {
public:
    Config() = default;

    bool load(const std::string& fileName) {
        auto filePath = (GetExecutablePath().parent_path() / fileName);
        if (!std::filesystem::exists(filePath)) {
            std::ofstream file(filePath);
            for (std::string string : defaultConfig) {
                file << string << "\n";
            }
            file.close();
        }
        std::ifstream file(filePath);
        if (!file.is_open()) return false;

        std::string line;
        while (std::getline(file, line)) {
            std::erase_if(line, ::isspace);

            if (line.empty() || line[0] == '#') continue;

            const size_t eq = line.find('=');
            if (eq == std::string::npos) continue;

            std::string key = line.substr(0, eq);
            data[key] = line.substr(eq + 1);
        }

        return true;
    }

    bool has(const std::string& key) const {
        return data.contains(key);
    }

    std::string getString(const std::string& key, const std::string& def = "") const {
        const auto it = data.find(key);
        return (it != data.end()) ? it->second : def;
    }

    int getInt(const std::string& key, const int def = 0) const {
        const auto it = data.find(key);
        return (it != data.end()) ? std::stoi(it->second) : def;
    }

    bool getBool(const std::string& key, const bool def = false) const {
        const auto it = data.find(key);
        if (it == data.end()) return def;

        std::string v = it->second;
        std::ranges::transform(v, v.begin(), ::tolower);

        return (v == "1" || v == "true" || v == "yes");
    }

private:
    std::unordered_map<std::string, std::string> data;
};