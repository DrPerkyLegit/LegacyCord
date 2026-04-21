package dev.drperky.networking.pipelines.clientbound;

import dev.drperky.networking.datatypes.LCEConnection;
import dev.drperky.networking.datatypes.LCEPacket;
import dev.drperky.networking.pipelines.NetworkPipeline;
import dev.drperky.networking.pipelines.NetworkPipelineContext;
import dev.drperky.utils.ByteBufferExtras;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ProxyMessagePacketPipeline implements NetworkPipeline {
    @Override
    public boolean pipe(NetworkPipelineContext ctx) throws IOException {
        LCEConnection connection = ctx.getConnection();
        boolean isTraveling = connection.getTravelData().isTraveling();

        for (LCEPacket packet : ctx.getPackets()) {
            ByteBuffer packetBuffer = packet.getPacketBuffer();
            packetBuffer.mark();

            if (!isTraveling && packet.getPacketId() == 250 && !ctx.isServerbound()) {
                String identifier = ByteBufferExtras.readUtf(packetBuffer, 20);
                if (identifier.equals("PM|Transfer")) {
                    //Custom Payload Packet that sends the ip and port we are traveling to, server -> client packet
                    String serverAddress = ByteBufferExtras.readUtf(packetBuffer, 128);
                    int serverPort = packetBuffer.getInt();

                    //todo: emit transfer event and check canceled
                    ctx.addDroppedPacket(packet);
                    connection.getTravelData().queueTravel(serverAddress, serverPort);
                }
            } else if (isTraveling && !ctx.isServerbound()) {
                if (packet.getPacketId() == 1) {
                    int entityId = packetBuffer.getInt();
                    ByteBufferExtras.readUtf(packetBuffer, 20); //skip username

                    String levelType = ByteBufferExtras.readUtf(packetBuffer, 16); //
                    long seed = packetBuffer.getLong(); //
                    int gameType = packetBuffer.getInt(); //
                    byte dimension = packetBuffer.get(); //
                    byte mapHeight = packetBuffer.get(); //

                    packetBuffer.get(); //skip maxPlayers
                    packetBuffer.getLong(); //skip m_offlineXuid
                    packetBuffer.getLong(); //skip m_onlineXuid
                    packetBuffer.get(); //skip m_friendsOnlyUGC
                    packetBuffer.getInt(); //skip m_ugcPlayersVersion

                    byte difficulty = packetBuffer.get(); //
                    packetBuffer.getInt(); //skip m_multiplayerInstanceId
                    packetBuffer.get(); //skip m_playerIndex
                    packetBuffer.getInt(); //skip m_playerSkinId
                    packetBuffer.getInt(); //skip m_playerCapeId
                    packetBuffer.get(); //skip m_isGuest

                    byte newSeaLevel = packetBuffer.get();
                    packetBuffer.getInt(); //skip m_uiGamePrivileges //todo: send this as another packet to the client
                    //todo: look into ServerSettingsChangedPacket, it handles host option changes, and difficulty changes

                    short xzSize = packetBuffer.getShort();
                    byte hellScale = packetBuffer.get();

                    {
                        //tell the client new info, set dim to the nether, server will correct the dim and client will get new info
                        int newPacketSize = 13 + levelType.length();
                        ByteBuffer newBuffer = ByteBuffer.allocate(4 + newPacketSize);
                        newBuffer.putInt(newPacketSize);
                        newBuffer.put((byte)9);

                        newBuffer.put((byte)(dimension == -1 ? 0 : -1));
                        newBuffer.put((byte)gameType);
                        newBuffer.putShort(mapHeight);
                        ByteBufferExtras.writeUtf(packetBuffer, levelType, 16);
                        newBuffer.putLong(seed);
                        newBuffer.put(difficulty);
                        newBuffer.put(newSeaLevel);
                        newBuffer.putShort((short)entityId);
                        newBuffer.putShort(xzSize);
                        newBuffer.put(hellScale);
                        connection.getServerChannel().write(newBuffer);
                    }

                    connection.getTravelData().finishTraveling();
                } else if (packet.getPacketId() == 2) {
                    connection.getServerChannel().write(connection.getTravelData().getCachedPacket_Login().CompressPacket());
                }
            } else if (!isTraveling && ctx.isServerbound() && !connection.getTravelData().hasTraveledOnce()) {
                if (packet.getPacketId() == 1 && connection.getTravelData().getCachedPacket_Login() == null) {
                    connection.getTravelData().setCachedPacket_Login(packet);
                } else if (packet.getPacketId() == 2 && connection.getTravelData().getCachedPacket_PreLogin() == null) {
                    connection.getTravelData().setCachedPacket_PreLogin(packet);
                }
            }

            packetBuffer.reset();
        }

        ctx.dropPackets();
        return true;
    }
}
