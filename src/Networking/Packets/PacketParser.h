//
// Created by PeakB on 4/14/2026.
//
#pragma once
#include <unordered_map>
#include <vector>

#include "LCEPacket.h"
#include "LegacyCord.h"

class PacketParser {
public:
    PacketParser(PlayerConnection* connection) { //skin packets can be 64kb, idk any bigger packets
        serverBuffer = new char[LCEPacket::MAXPACKETSIZE];
        serverOffset = 0;

        clientBuffer = new char[LCEPacket::MAXPACKETSIZE];
        clientOffset = 0;

        associatedConnection = connection;
    }
    ~PacketParser() {
        delete[] serverBuffer;
        delete[] clientBuffer;
    }

    void feed(const char* data, size_t size, bool fromServer) {
        char* buffer = fromServer ? serverBuffer : clientBuffer;
        size_t& offset = fromServer ? serverOffset : clientOffset;

        if (offset + size > LCEPacket::MAXPACKETSIZE) {
            LegacyCord::getLogger()->Debug("Packet hit max buffer limit, are we leaking?: ", offset, " | ", size);
            offset = 0;
            return;
        }

        memcpy(buffer + offset, data, size);
        offset += size;
        while (true) {
            if (offset < LCEPacket::HEADERSIZE) return;

            int packetSize =
                (static_cast<uint8_t>(buffer[0]) << 24) |
                (static_cast<uint8_t>(buffer[1]) << 16) |
                (static_cast<uint8_t>(buffer[2]) << 8) |
                (static_cast<uint8_t>(buffer[3]));

            static int resyncCount = 0;

            if (packetSize <= 0 || packetSize > LCEPacket::MAXPACKETSIZE) {
                //todo: add a limit to pushing back offset and throw socket error
                resyncCount++;
                memmove(buffer, buffer + 1, offset - 1);
                offset--;

                LegacyCord::getLogger()->Debug("Packet Buffer Desync Fix Attempt: ", resyncCount);
                continue;
            }

            size_t total = packetSize + LCEPacket::HEADERSIZE;
            if (offset < total) return;

            std::shared_ptr<LCEPacket> newPacket = std::make_shared<LCEPacket>(buffer);
            LegacyCord::getNetworkManager()->handlePlayerPacket(associatedConnection, newPacket, fromServer);

            //LegacyCord::getLogger()->Debug((fromServer ? "[S2C]"  : "[C2S]"), " ID:", static_cast<int>(static_cast<uint8_t>(buffer[4])), " Size: ", packetSize);

            size_t remaining = offset - total;
            memmove(buffer, buffer + total, remaining);
            offset = remaining;
        }
    }
private:
    char* serverBuffer;
    size_t serverOffset;

    char* clientBuffer;
    size_t clientOffset;

    PlayerConnection* associatedConnection;
};
