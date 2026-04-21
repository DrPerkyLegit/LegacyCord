package dev.drperky.events.core.datatypes;

import dev.drperky.networking.datatypes.LCEConnection;
import dev.drperky.networking.datatypes.LCEPacket;

public class PacketEvent implements Event {
    public final LCEConnection connection;
    public final LCEPacket packet;
    public final boolean serverbound;

    public PacketEvent(LCEConnection connection, LCEPacket packet, boolean serverbound) {
        this.connection = connection;
        this.packet = packet;
        this.serverbound = serverbound;
    }
}
