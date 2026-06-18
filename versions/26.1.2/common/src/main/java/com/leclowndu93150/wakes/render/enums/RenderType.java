package com.leclowndu93150.wakes.render.enums;

import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.config.WakesConfig;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public enum RenderType {
    AUTO(null),
    TRANSLUCENT_BLOCK(RenderPipelines.TRANSLUCENT_BLOCK),
    BEACON_BEAM_TRANSLUCENT(RenderPipelines.BEACON_BEAM_TRANSLUCENT);

    public static final RenderPipeline WAKE_TRANSLUCENT_LIT = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("wakes", "pipeline/wake_translucent_lit"))
            .withVertexShader("core/block")
            .withFragmentShader("core/block")
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("Fog", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS)
            .withShaderDefine("ALPHA_CUTOUT", 0.01F)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .build();

    public final RenderPipeline pipeline;

    RenderType(RenderPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public static RenderPipeline getPipeline() {
        RenderType configured = WakesConfig.DEBUG.renderType.get();
        if (configured == RenderType.AUTO) {
            if (WakesClient.areShadersEnabled) {
                return TRANSLUCENT_BLOCK.pipeline;
            } else {
                return WAKE_TRANSLUCENT_LIT;
            }
        }
        if (configured == RenderType.BEACON_BEAM_TRANSLUCENT) {
            return WAKE_TRANSLUCENT_LIT;
        }
        return configured.pipeline;
    }
}
