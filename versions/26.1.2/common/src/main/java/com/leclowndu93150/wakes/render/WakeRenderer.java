package com.leclowndu93150.wakes.render;

import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.compat.iris.IrisAccess;
import com.leclowndu93150.wakes.render.enums.RenderType;
import com.leclowndu93150.wakes.render.water.ShaderWaterHeight;
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
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class WakeRenderer {

    public static void render(PoseStack poseStack, Frustum frustum, Vec3 cameraPos) {
        WakesClient.areShadersEnabled = IrisAccess.shadersEnabled();

        if (WakesConfig.GENERAL.disableMod.get()) {
            WakesDebugInfo.quadsRendered = 0;
            return;
        }

        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null || WakeHandler.resolutionResetScheduled) return;

        ArrayList<Brick> bricks = wakeHandler.getVisible(frustum, Brick.class);
        if (bricks.isEmpty()) return;

        long tRendering = System.nanoTime();

        ArrayList<Brick> ready = new ArrayList<>(bricks.size());
        for (Brick brick : bricks) {
            if (brick.imgPtr == -1) continue;
            if (brick.pixelsStale) {
                brick.populatePixels();
            }
            if (brick.wakeTexture == null) {
                brick.wakeTexture = new WakeTexture(WakeHandler.resolution.res, true, QuadTree.BRICK_WIDTH);
            }
            if (brick.pixelsDirty) {
                brick.wakeTexture.loadTexture(brick.imgPtr);
                brick.pixelsDirty = false;
            }
            ready.add(brick);
        }
        if (ready.isEmpty()) {
            WakesDebugInfo.quadsRendered = 0;
            return;
        }

        RenderPipeline pipeline = RenderType.getPipeline();
        Matrix4f matrix = poseStack.last().pose();
        float heightOffset = ShaderWaterHeight.offset();

        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        BlockPos.MutableBlockPos lightPos = new BlockPos.MutableBlockPos();

        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, pipeline.getVertexFormat());
        for (Brick brick : ready) {
            Vector3f pos = brick.pos.add(cameraPos.reverse()).toVector3f().add(0, WakeNode.WATER_OFFSET + heightOffset, 0);
            float dim = brick.dim;

            int bx = (int) Math.floor(brick.pos.x);
            int by = (int) Math.floor(brick.pos.y);
            int bz = (int) Math.floor(brick.pos.z);
            int light00 = lightAt(level, lightPos, bx, by, bz);
            int light01 = lightAt(level, lightPos, bx, by, bz + (int) dim);
            int light11 = lightAt(level, lightPos, bx + (int) dim, by, bz + (int) dim);
            int light10 = lightAt(level, lightPos, bx + (int) dim, by, bz);

            bb.addVertex(matrix, pos.x, pos.y, pos.z)
                    .setUv(0, 0).setColor(1f, 1f, 1f, 1f)
                    .setLight(light00).setNormal(0f, 1f, 0f);
            bb.addVertex(matrix, pos.x, pos.y, pos.z + dim)
                    .setUv(0, 1).setColor(1f, 1f, 1f, 1f)
                    .setLight(light01).setNormal(0f, 1f, 0f);
            bb.addVertex(matrix, pos.x + dim, pos.y, pos.z + dim)
                    .setUv(1, 1).setColor(1f, 1f, 1f, 1f)
                    .setLight(light11).setNormal(0f, 1f, 0f);
            bb.addVertex(matrix, pos.x + dim, pos.y, pos.z)
                    .setUv(1, 0).setColor(1f, 1f, 1f, 1f)
                    .setLight(light10).setNormal(0f, 1f, 0f);
        }

        MeshData built = bb.buildOrThrow();
        GpuBuffer vertexBuffer = pipeline.getVertexFormat().uploadImmediateVertexBuffer(built.vertexBuffer());

        GpuSampler sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
        RenderSystem.AutoStorageIndexBuffer seqBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer quadIndexBuffer = seqBuffer.getBuffer(6);

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1f, 1f, 1f, 1f), new Vector3f(), new Matrix4f());

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> WakesClient.MOD_ID + " wake render",
                client.getMainRenderTarget().getColorTextureView(),
                OptionalInt.empty(),
                client.getMainRenderTarget().getDepthTextureView(),
                OptionalDouble.empty())) {

            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamicTransforms);
            pass.bindTexture("Sampler2", client.gameRenderer.lightmap(), sampler);
            pass.setVertexBuffer(0, vertexBuffer);
            pass.setIndexBuffer(quadIndexBuffer, seqBuffer.type());
            for (int i = 0; i < ready.size(); i++) {
                pass.bindTexture("Sampler0", ready.get(i).wakeTexture.getTextureView(), sampler);
                pass.drawIndexed(i * 4, 0, 6, 1);
            }
        }
        built.close();

        WakesDebugInfo.renderingTime.add(System.nanoTime() - tRendering);
        WakesDebugInfo.quadsRendered = ready.size();
    }

    private static int lightAt(ClientLevel level, BlockPos.MutableBlockPos pos, int x, int y, int z) {
        if (level == null) return LightCoordsUtil.FULL_BRIGHT;
        pos.set(x, y + 1, z);
        if (!level.hasChunkAt(pos)) return LightCoordsUtil.FULL_BRIGHT;
        int block = level.getBrightness(LightLayer.BLOCK, pos);
        int sky = level.getBrightness(LightLayer.SKY, pos);
        return LightCoordsUtil.pack(block, sky);
    }
}
