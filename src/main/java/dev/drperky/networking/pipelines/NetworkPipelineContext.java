package dev.drperky.networking.pipelines;

import dev.drperky.networking.datatypes.LCEConnection;
import dev.drperky.networking.datatypes.LCEPacket;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class NetworkPipelineContext {
    private final LCEConnection connection;
    private ByteBuffer rawBuffer;

    private List<LCEPacket> packets;
    private List<LCEPacket> packetsToDrop;

    private boolean serverbound;

    public NetworkPipelineContext(LCEConnection connection, ByteBuffer rawBuffer, boolean serverbound) {
        this.connection = connection;
        this.rawBuffer = rawBuffer;

        this.packets = new ArrayList<>();
        this.packetsToDrop = new ArrayList<>();

        this.serverbound = serverbound;
    }

    public boolean isServerbound() {
        return this.serverbound;
    }

    public ByteBuffer getRawBuffer() {
        return this.rawBuffer;
    }

    public LCEConnection getConnection() {
        return this.connection;
    }

    public List<LCEPacket> getPackets() {
        return this.packets;
    }

    public void addDroppedPacket(LCEPacket packet) {
        this.packetsToDrop.add(packet);
    }

    public void dropPackets() {
        this.packets.removeAll(this.packetsToDrop);
        this.packetsToDrop.clear();
    }

    public void setPackets(List<LCEPacket> packets) {
        this.packets = packets;
    }
}
