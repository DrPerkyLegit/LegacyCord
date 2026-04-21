package dev.drperky.networking.datatypes;

import java.nio.ByteBuffer;

public class LCEPacket {
    public static int HEADERSIZE = 4;
    public static int MAXSIZE = 64*1024;

    int dataSize;
    int packetId;
    ByteBuffer data;

    public LCEPacket(int dataSize, int packetId, ByteBuffer data) {
        this.dataSize = dataSize;
        this.packetId = packetId;

        this.data = data.duplicate();
        this.data.rewind();
        this.data.limit(dataSize);
    }

    @Override
    public LCEPacket clone() {
        byte[] copy = new byte[dataSize];

        ByteBuffer dup = data.duplicate();
        dup.rewind();
        dup.get(copy);

        return new LCEPacket(dataSize, packetId, ByteBuffer.wrap(copy));
    }

    public int getDataSize() { return dataSize; }
    public int getPacketId() { return packetId; }
    public ByteBuffer getPacketBuffer() { return data; }

    //todo: find a way that doesnt do allocation
    public ByteBuffer CompressPacket() {
        ByteBuffer out = ByteBuffer.allocate(4 + 1 + dataSize);

        out.putInt(dataSize + 1);
        out.put((byte) packetId);

        ByteBuffer dup = data.duplicate();
        dup.rewind();
        dup.limit(dataSize);

        out.put(dup);

        out.flip();
        return out;
    }
}
