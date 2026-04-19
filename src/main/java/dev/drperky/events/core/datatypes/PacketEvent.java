package dev.drperky.events.core.datatypes;

import dev.drperky.networking.datatypes.PlayerConnection;
import dev.drperky.networking.packets.LCEPacket;

public class PacketEvent implements Event {
    public final PlayerConnection connection;
    public final LCEPacket packet;
    public final boolean serverbound;

    public PacketEvent(PlayerConnection connection, LCEPacket packet, boolean serverbound) {
        this.connection = connection;
        this.packet = packet;
        this.serverbound = serverbound;
    }
}
