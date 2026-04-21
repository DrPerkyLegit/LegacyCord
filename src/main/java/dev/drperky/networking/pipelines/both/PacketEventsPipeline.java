package dev.drperky.networking.pipelines.both;

import dev.drperky.networking.datatypes.LCEPacket;
import dev.drperky.networking.pipelines.NetworkPipeline;
import dev.drperky.networking.pipelines.NetworkPipelineContext;

public class PacketEventsPipeline implements NetworkPipeline {
    @Override
    public boolean pipe(NetworkPipelineContext ctx) {
        for (LCEPacket packet : ctx.getPackets()) {

        }

        return true;
    }
}
