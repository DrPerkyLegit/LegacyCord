//
// Created by PeakB on 4/14/2026.
//
#pragma once
#include <cstdint>
#include <cstring>


class LCEPacket {
public:
    //size in bytes
    static const int MAXPACKETSIZE = 64*1024;
    static const int HEADERSIZE = 0x04;


    LCEPacket(const char* buffer) {
        this->size = ((static_cast<uint8_t>(buffer[0]) << 24) | (static_cast<uint8_t>(buffer[1]) << 16) | (static_cast<uint8_t>(buffer[2]) << 8) | (static_cast<uint8_t>(buffer[3])));
        this->packetId = static_cast<uint8_t>(buffer[4]);

        this->data = new char[(this->size - 1)];
        memcpy(this->data, buffer + 5, (this->size - 1));
    };

    LCEPacket(int packetSize) {
        this->size = 0;
        this->packetId = 0;
        this->data = new char[packetSize];
    };

    ~LCEPacket() {
        delete[] data;
    }

    uint8_t getId() const {
        return packetId;
    }

    int getSize() const {
        return size;
    }

    char* getRawData() {
        return data;
    }

    bool writeInt(int32_t value, int offset) {
        data[offset] = (value >> 24) & 0xFF;
        data[offset+1] = (value >> 16) & 0xFF;
        data[offset+2] = (value >> 8) & 0xFF;
        data[offset+3] = value & 0xFF;
        return true;
    }

    int32_t readInt(int offset) {
        int32_t value =
            (data[offset] << 24) |
            (data[offset+1] << 16) |
            (data[offset+2] << 8) |
            (data[offset+3]);

        return value;
    }

    bool writeShort(short value, int offset) {
        data[offset] = (value >> 8) & 0xFF;
        data[offset+1] = value & 0xFF;
        return true;
    }

    short readShort(int offset) {
        int16_t value = static_cast<int16_t>(((data[offset] << 8) | (data[offset + 1])));
        return value;
    }

    std::wstring readWString(int offset) {
        std::wstring results; int16_t stringLength = readShort(offset);

        for (int16_t i = 0 ; i < stringLength; i++) {
            int16_t _char = readShort(offset + (i * 0x02));
            results += static_cast<wchar_t>(_char);
        }
        return results;
    }

private:
    int size;
    uint8_t packetId;

    char* data;
    //int offset;
};