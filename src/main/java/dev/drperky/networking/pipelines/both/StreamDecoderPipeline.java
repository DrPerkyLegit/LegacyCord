package dev.drperky.networking.pipelines.both;

import dev.drperky.networking.datatypes.LCEPacket;
import dev.drperky.networking.pipelines.NetworkPipeline;
import dev.drperky.networking.pipelines.NetworkPipelineContext;

import java.io.IOException;
import java.util.List;

public class StreamDecoderPipeline implements NetworkPipeline {
    @Override
    public boolean pipe(NetworkPipelineContext ctx) {
        ctx.setPackets((ctx.isServerbound() ? ctx.getConnection().getClientReader() : ctx.getConnection().getServerReader()).feed(ctx.getRawBuffer(), ctx.isServerbound()));
        return true;
    }
}
