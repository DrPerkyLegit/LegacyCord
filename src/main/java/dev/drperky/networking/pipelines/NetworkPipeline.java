package dev.drperky.networking.pipelines;

import dev.drperky.networking.datatypes.LCEConnection;

import java.io.IOException;

public interface NetworkPipeline {
    boolean pipe(NetworkPipelineContext ctx) throws Exception;
}
