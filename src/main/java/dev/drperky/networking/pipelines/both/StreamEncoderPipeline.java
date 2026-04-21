package dev.drperky.networking.pipelines.both;

import dev.drperky.networking.datatypes.LCEConnection;
import dev.drperky.networking.datatypes.LCEPacket;
import dev.drperky.networking.pipelines.NetworkPipeline;
import dev.drperky.networking.pipelines.NetworkPipelineContext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class StreamEncoderPipeline implements NetworkPipeline {
    @Override
    public boolean pipe(NetworkPipelineContext ctx) throws IOException {
        LCEConnection connection = ctx.getConnection();
        SocketChannel socketChannel = (ctx.isServerbound() ? connection.getServerChannel() : connection.getClientChannel());

        if (connection.getTravelData().getCachedPacket_PreLogin() == null) {
            socketChannel.write(ctx.getRawBuffer());
        } else {
            ctx.dropPackets(); //drop any packets that someone forgot to drop in a pipeline

            for (LCEPacket packet : ctx.getPackets()) {
                if (!connection.getTravelData().isTraveling()) {
                    socketChannel.write(packet.CompressPacket());
                }
            }
        }
        return false;
    }
}
