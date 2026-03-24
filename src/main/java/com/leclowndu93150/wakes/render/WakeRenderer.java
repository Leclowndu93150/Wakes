package com.leclowndu93150.wakes.render;

import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.config.enums.Resolution;
import com.leclowndu93150.wakes.render.enums.RenderType;
import com.leclowndu93150.wakes.simulation.Brick;
import com.leclowndu93150.wakes.simulation.QuadTree;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.simulation.WakeNode;
import com.leclowndu93150.wakes.debug.WakesDebugInfo;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

@EventBusSubscriber(value = Dist.CLIENT)
public class WakeRenderer {
    public static Map<Resolution, WakeTexture> wakeTextures = null;

    private static void initTextures() {
        wakeTextures = Map.of(
                Resolution.EIGHT, new WakeTexture(Resolution.EIGHT.res, true, QuadTree.BRICK_WIDTH),
                Resolution.SIXTEEN, new WakeTexture(Resolution.SIXTEEN.res, true, QuadTree.BRICK_WIDTH),
                Resolution.THIRTYTWO, new WakeTexture(Resolution.THIRTYTWO.res, true, QuadTree.BRICK_WIDTH)
        );
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        if (WakesConfig.GENERAL.disableMod.get()) {
            WakesDebugInfo.quadsRendered = 0;
            return;
        }

        if (wakeTextures == null) initTextures();

        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null || WakeHandler.resolutionResetScheduled) return;

        CameraRenderState camera = event.getLevelRenderState().cameraRenderState;
        ArrayList<Brick> bricks = wakeHandler.getVisible(camera.cullFrustum, Brick.class);
        if (bricks.isEmpty()) return;

        RenderPipeline pipeline = RenderType.getPipeline();
        Matrix4f matrix = event.getPoseStack().last().pose();
        Vec3 cameraPos = camera.pos;

        Resolution resolution = WakeHandler.resolution;
        WakeTexture texture = wakeTextures.get(resolution);
        int n = 0;
        long tRendering = System.nanoTime();

        GpuSampler sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
        Minecraft client = Minecraft.getInstance();
        RenderSystem.AutoStorageIndexBuffer seqBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer quadIndexBuffer = seqBuffer.getBuffer(6);

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1f, 1f, 1f, 1f), new Vector3f(), new Matrix4f());

        for (Brick brick : bricks) {
            if (!brick.hasPopulatedPixels) continue;
            texture.loadTexture(brick.imgPtr);

            Vector3f pos = brick.pos.add(cameraPos.reverse()).toVector3f().add(0, WakeNode.WATER_OFFSET, 0);
            float dim = brick.dim;

            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, pipeline.getVertexFormat());
            bb.addVertex(matrix, pos.x, pos.y, pos.z)
                    .setUv(0, 0).setColor(1f, 1f, 1f, 1f)
                    .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(0f, 1f, 0f);
            bb.addVertex(matrix, pos.x, pos.y, pos.z + dim)
                    .setUv(0, 1).setColor(1f, 1f, 1f, 1f)
                    .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(0f, 1f, 0f);
            bb.addVertex(matrix, pos.x + dim, pos.y, pos.z + dim)
                    .setUv(1, 1).setColor(1f, 1f, 1f, 1f)
                    .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(0f, 1f, 0f);
            bb.addVertex(matrix, pos.x + dim, pos.y, pos.z)
                    .setUv(1, 0).setColor(1f, 1f, 1f, 1f)
                    .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(0f, 1f, 0f);

            MeshData built = bb.buildOrThrow();
            GpuBuffer vertexBuffer = pipeline.getVertexFormat().uploadImmediateVertexBuffer(built.vertexBuffer());

            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> WakesClient.MOD_ID + " wake render",
                    client.getMainRenderTarget().getColorTextureView(),
                    OptionalInt.empty(),
                    client.getMainRenderTarget().getDepthTextureView(),
                    OptionalDouble.empty())) {

                pass.setPipeline(pipeline);
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", dynamicTransforms);
                pass.bindTexture("Sampler0", texture.getTextureView(), sampler);
                pass.bindTexture("Sampler2", client.gameRenderer.lightmap(), sampler);
                pass.setVertexBuffer(0, vertexBuffer);
                pass.setIndexBuffer(quadIndexBuffer, seqBuffer.type());
                pass.drawIndexed(0, 0, 6, 1);
            }
            built.close();
            n++;
        }

        WakesDebugInfo.renderingTime.add(System.nanoTime() - tRendering);
        WakesDebugInfo.quadsRendered = n;
    }
}
