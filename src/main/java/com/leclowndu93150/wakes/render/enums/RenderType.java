package com.leclowndu93150.wakes.render.enums;


import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.config.WakesConfig;
import java.util.function.Supplier;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;

public enum RenderType {
    AUTO(null),
    GENERAL(GameRenderer::getPositionColorTexLightmapShader),
    CUSTOM(WakesClient.TRANSLUCENT_NO_LIGHT_DIRECTION_PROGRAM::getProgram),
    SOLID(GameRenderer::getRendertypeSolidShader),
    TRANSLUCENT(GameRenderer::getRendertypeTranslucentShader),
    CUTOUT(GameRenderer::getRendertypeCutoutShader),
    ENTITY_SOLID(GameRenderer::getRendertypeEntitySolidShader),
    ENTITY_TRANSLUCENT(GameRenderer::getRendertypeEntityTranslucentShader),
    ENTITY_TRANSLUCENT_CULL(GameRenderer::getRendertypeEntityTranslucentCullShader),
    ENTITY_CUTOUT(GameRenderer::getRendertypeEntityCutoutShader),
    ENTITY_CUTOUT_NO_CULL(GameRenderer::getRendertypeEntityCutoutNoCullShader),
    ENTITY_CUTOUT_NO_CULL_Z_OFFSET(GameRenderer::getRendertypeEntityCutoutNoCullZOffsetShader)
    ;

    public final Supplier<ShaderInstance> program;

    RenderType(Supplier<ShaderInstance> program) {
        this.program = program;
    }

    public static Supplier<ShaderInstance> getProgram() {
        if (WakesConfig.renderType == RenderType.AUTO) {
            if (WakesClient.areShadersEnabled) {
                return ENTITY_TRANSLUCENT_CULL.program;
            } else {
                return CUSTOM.program;
            }
        }
        return WakesConfig.renderType.program;
    }
}
