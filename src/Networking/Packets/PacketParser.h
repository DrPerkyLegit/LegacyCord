//
// Created by PeakB on 4/14/2026.
//
#pragma once
#include <unordered_map>
#include <vector>

#include "LCEPacket.h"

class PacketParser {
public:
    PacketParser() { //skin packets can be 64kb, idk any bigger packets
        serverBuffer = new char[LCEPacket::MAXPACKETSIZE];
        serverOffset = 0;

        clientBuffer = new char[LCEPacket::MAXPACKETSIZE];
        clientOffset = 0;
    }
    ~PacketParser() {
        delete[] serverBuffer;
        delete[] clientBuffer;
    }

    bool feed(const char* data, size_t size, bool fromServer) {
        char* buffer = fromServer ? serverBuffer : clientBuffer;
        size_t& offset = fromServer ? serverOffset : clientOffset;

        if (offset + size > LCEPacket::MAXPACKETSIZE) {
            offset = 0;
            return false;
        }

        memcpy(buffer + offset, data, size);
        offset += size;

        if (offset <= LCEPacket::HEADERSIZE) return true; //not enough data to parse anything

        int packetSize = ((static_cast<uint8_t>(buffer[0]) << 24) | (static_cast<uint8_t>(buffer[1]) << 16) | (static_cast<uint8_t>(buffer[2]) << 8) | (static_cast<uint8_t>(buffer[3])));
        if ((packetSize + LCEPacket::HEADERSIZE) > offset) return true;

        std::shared_ptr<LCEPacket> newPacket = std::make_shared<LCEPacket>(buffer);

        //handle full packet here

        offset = 0;
        return true;
    }
private:
    char* serverBuffer;
    size_t serverOffset;

    char* clientBuffer;
    size_t clientOffset;
};
