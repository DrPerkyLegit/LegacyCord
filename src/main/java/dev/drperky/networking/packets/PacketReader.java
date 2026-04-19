package dev.drperky.networking.packets;

import dev.drperky.LegacyCord;
import dev.drperky.events.core.datatypes.PacketEvent;
import dev.drperky.networking.NetworkManager;
import dev.drperky.networking.datatypes.PlayerConnection;
import dev.drperky.utils.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PacketReader {
    private final ByteBuffer pendingBuffer = ByteBuffer.allocate(32*1024);
    private final PlayerConnection assignedConnection;
    private int streamErrors = 0;

    public PacketReader(PlayerConnection connection) {
        this.assignedConnection = connection;
    }

    public boolean hasTooManyStreamErrors() {
        return streamErrors > 4;
    }

    public void clear() {
        pendingBuffer.clear();
        streamErrors = 0;
    }

    public void feed(ByteBuffer data, boolean serverbound) {
        int len = data.remaining();
        if (len <= 0) return;

        byte[] tmp = new byte[len];
        data.mark(); data.get(tmp); data.reset();

        pendingBuffer.put(tmp);
        pendingBuffer.flip();

        while (pendingBuffer.remaining() > 4 && !hasTooManyStreamErrors()) {
            pendingBuffer.mark();

            int packetSize = pendingBuffer.getInt();

            if (packetSize <= 0 || packetSize > 131072) {
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

            ByteBuffer packetBuffer = pendingBuffer.slice();
            packetBuffer.limit(packetSize - 1);

            LCEPacket newPacket = new LCEPacket(packetSize - 1, packetId, packetBuffer);
            try {
                boolean isTraveling = assignedConnection.isTraveling();
                if (isTraveling && !serverbound) {
                    if (packetId == 1) {
                        pendingBuffer.mark();
                        int entityId = pendingBuffer.getInt();
                        this.readUtf(20); //skip username

                        String levelType = this.readUtf(16); //
                        long seed = pendingBuffer.getLong(); //
                        int gameType = pendingBuffer.getInt(); //
                        byte dimension = pendingBuffer.get(); //
                        byte mapHeight = pendingBuffer.get(); //

                        pendingBuffer.get(); //skip maxPlayers
                        pendingBuffer.getLong(); //skip m_offlineXuid
                        pendingBuffer.getLong(); //skip m_onlineXuid
                        pendingBuffer.get(); //skip m_friendsOnlyUGC
                        pendingBuffer.getInt(); //skip m_ugcPlayersVersion

                        int difficulty = pendingBuffer.get(); //
                        pendingBuffer.getInt(); //skip m_multiplayerInstanceId
                        pendingBuffer.get(); //skip m_playerIndex
                        pendingBuffer.getInt(); //skip m_playerSkinId
                        pendingBuffer.getInt(); //skip m_playerCapeId
                        pendingBuffer.get(); //skip m_isGuest

                        boolean newSeaLevel = pendingBuffer.get() != 0;
                        pendingBuffer.getInt(); //skip m_uiGamePrivileges //todo: send this as another packet to the client

                        short xzSize = pendingBuffer.getShort();
                        byte hellScale = pendingBuffer.get();

                        pendingBuffer.reset();
                        {
                            //tell the client new info, set dim to the nether, server will correct the dim and client will get new info

                            int newPacketSize = 13 + levelType.length();
                            ByteBuffer newBuffer = ByteBuffer.allocate(4 + newPacketSize);
                            newBuffer.putInt(newPacketSize);
                            newBuffer.put((byte)9);

                            newBuffer.put((byte)(dimension == -1 ? 0 : -1));
                            newBuffer.put((byte)gameType);
                            newBuffer.putShort(mapHeight);
                            this.writeUtf(levelType, 16);
                            newBuffer.putLong(seed);
                            newBuffer.put((byte)difficulty);
                            newBuffer.put((byte)(newSeaLevel ? 1 : 0));
                            newBuffer.putShort((short)entityId);
                            newBuffer.putShort((short)xzSize);
                            newBuffer.put((byte)hellScale);
                            assignedConnection.getServerChannel().write(newBuffer);
                        }

                        assignedConnection.finishTraveling();
                    } else if (packetId == 2) {
                        assignedConnection.getServerChannel().write(assignedConnection.getCachedPacket_Login().CompressPacket());
                    }
                } else if (!isTraveling && serverbound && !assignedConnection.hasTraveledOnce()) {
                    if (packetId == 1 && assignedConnection.getCachedPacket_Login() == null) {
                        assignedConnection.setCachedPacket_Login(newPacket);
                    } else if (packetId == 2 && assignedConnection.getCachedPacket_PreLogin() == null) {
                        assignedConnection.setCachedPacket_PreLogin(newPacket);
                    }
                }
            } catch(Exception e) {}

            LegacyCord.getEventBus().fire(new PacketEvent(this.assignedConnection, newPacket, serverbound));

            //Logger.Info((serverbound ? "[C2S]" : "[S2C]"), " ID: ", String.valueOf(newPacket.getPacketId()), " Size: ", String.valueOf(newPacket.getDataSize()));

            pendingBuffer.position(startPos + packetSize);
        }

        pendingBuffer.compact();
    }

    public String readUtf(int maxLength) {
        short stringLength = pendingBuffer.getShort();
        if (stringLength > maxLength || stringLength <= 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder(stringLength);
        for (int i = 0; i < stringLength; i++) {
            char c = pendingBuffer.getChar();
            builder.append(c);
        }

        return builder.toString();
    }

    public void writeUtf(String value, int maxLength) throws IOException {
        if (value == null) value = "";

        int length = value.length();
        if (length > maxLength) {
            throw new IOException("String length " + length + " exceeds max of " + maxLength);
        }

        pendingBuffer.putShort((short) length);
        for (int i = 0; i < length; i++) {
            pendingBuffer.putChar(value.charAt(i));
        }
    }
}
