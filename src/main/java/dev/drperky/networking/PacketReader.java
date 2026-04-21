package dev.drperky.networking;

import dev.drperky.LegacyCord;
import dev.drperky.events.core.datatypes.PacketEvent;
import dev.drperky.networking.datatypes.LCEConnection;
import dev.drperky.networking.datatypes.LCEPacket;
import dev.drperky.utils.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class PacketReader {
    private final ByteBuffer pendingBuffer = ByteBuffer.allocate(LCEPacket.MAXSIZE);
    private final LCEConnection assignedConnection;
    private int streamErrors = 0;

    public PacketReader(LCEConnection connection) {
        this.assignedConnection = connection;
    }

    public boolean hasTooManyStreamErrors() {
        return streamErrors > 4;
    }

    public void clear() {
        pendingBuffer.clear();
        streamErrors = 0;
    }

    public List<LCEPacket> feed(ByteBuffer data, boolean serverbound) {
        List<LCEPacket> results = new ArrayList<>();

        int len = data.remaining();
        if (len <= 0) return results;

        data.mark();
        pendingBuffer.put(data);
        data.reset();

        pendingBuffer.flip();
        while (pendingBuffer.remaining() > LCEPacket.HEADERSIZE && !hasTooManyStreamErrors()) {
            pendingBuffer.mark();

            int packetSize = pendingBuffer.getInt();

            if (packetSize <= 0 || packetSize > LCEPacket.MAXSIZE) {
                pendingBuffer.reset();
                pendingBuffer.get(); // shift 1 byte
                streamErrors++;
                continue;
            }

            if (pendingBuffer.remaining() < packetSize) {
                pendingBuffer.reset(); break;
            }

            int startPos = pendingBuffer.position();
            int packetId = pendingBuffer.get() & 0xFF;

            byte[] packetData = new byte[packetSize - 1];
            pendingBuffer.get(packetData);


            results.add(new LCEPacket(packetSize - 1, packetId, ByteBuffer.wrap(packetData)));

            //if you want to log packets setup something inside stream decoder or encoder, or setup your own pipeline to check packets
            //Logger.Info((serverbound ? "[C2S]" : "[S2C]"), " ID: ", String.valueOf(newPacket.getPacketId()), " Size: ", String.valueOf(newPacket.getDataSize()));

            pendingBuffer.position(startPos + packetSize);
        }
        pendingBuffer.compact();

        return results;
    }
}
