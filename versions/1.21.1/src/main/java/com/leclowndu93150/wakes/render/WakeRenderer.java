package com.leclowndu93150.wakes.render;

import com.leclowndu93150.wakes.compat.ModCompat;
import com.leclowndu93150.wakes.compat.sable.SableCompat;
import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.simulation.Brick;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.render.water.ShaderWaterHeight;
import com.leclowndu93150.wakes.simulation.WakeNode;
import com.leclowndu93150.wakes.debug.WakesDebugInfo;
import net.minecraft.world.level.Level;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;

@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class WakeRenderer {
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        if (WakesConfig.GENERAL.disableMod.get()) {
            WakesDebugInfo.quadsRendered = 0;
            return;
        }

        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null || WakeHandler.resolutionResetScheduled) return;

        ArrayList<Brick> bricks = wakeHandler.getVisible(event.getFrustum(), Brick.class);

        Matrix4f matrix = event.getPoseStack().last().pose();
        RenderSystem.enableBlend();

        Minecraft.getInstance().gameRenderer.overlayTexture().setupOverlayColor();

        Level level = Minecraft.getInstance().level;
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        int n = 0;
        long tRendering = System.nanoTime();
        for (var brick : bricks) {
            render(matrix, event.getCamera(), brick, level, partialTick);
            n++;
        }
        WakesDebugInfo.renderingTime.add(System.nanoTime() - tRendering);
        WakesDebugInfo.quadsRendered = n;
    }

    private static void render(Matrix4f matrix, Camera camera, Brick brick, Level level, float partialTick) {
        if (brick.imgPtr == -1) return;
        if (brick.pixelsStale) {
            brick.populatePixels();
        }
        if (brick.wakeTexture == null) {
            brick.wakeTexture = new WakeTexture(WakeHandler.resolution.res, true);
        }
        if (brick.pixelsDirty) {
            brick.wakeTexture.upload(brick.imgPtr);
            brick.pixelsDirty = false;
        }
        brick.wakeTexture.bind();

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        Object subLevel = ModCompat.isSableLoaded() ? SableCompat.findSubLevelAtBlock(level, brick.pos.x, brick.pos.z) : null;

        if (subLevel != null) {
            double bx = brick.pos.x;
            double by = brick.pos.y + WakeNode.WATER_OFFSET + ShaderWaterHeight.offset();
            double bz = brick.pos.z;
            int dim = brick.dim;
            double cx = camera.getPosition().x;
            double cy = camera.getPosition().y;
            double cz = camera.getPosition().z;

            double[] c00 = SableCompat.transformPlotToGlobal(subLevel, bx, by, bz, partialTick);
            double[] c01 = SableCompat.transformPlotToGlobal(subLevel, bx, by, bz + dim, partialTick);
            double[] c11 = SableCompat.transformPlotToGlobal(subLevel, bx + dim, by, bz + dim, partialTick);
            double[] c10 = SableCompat.transformPlotToGlobal(subLevel, bx + dim, by, bz, partialTick);

            buffer.addVertex(matrix, (float)(c00[0] - cx), (float)(c00[1] - cy), (float)(c00[2] - cz))
                    .setColor(1f, 1f, 1f, 1f).setUv(0, 0);
            buffer.addVertex(matrix, (float)(c01[0] - cx), (float)(c01[1] - cy), (float)(c01[2] - cz))
                    .setColor(1f, 1f, 1f, 1f).setUv(0, 1);
            buffer.addVertex(matrix, (float)(c11[0] - cx), (float)(c11[1] - cy), (float)(c11[2] - cz))
                    .setColor(1f, 1f, 1f, 1f).setUv(1, 1);
            buffer.addVertex(matrix, (float)(c10[0] - cx), (float)(c10[1] - cy), (float)(c10[2] - cz))
                    .setColor(1f, 1f, 1f, 1f).setUv(1, 0);
        } else {
            Vector3f pos = brick.pos.add(camera.getPosition().reverse()).toVector3f().add(0, WakeNode.WATER_OFFSET + ShaderWaterHeight.offset(), 0);

            buffer.addVertex(matrix, pos.x, pos.y, pos.z)
                    .setColor(1f, 1f, 1f, 1f).setUv(0, 0);
            buffer.addVertex(matrix, pos.x, pos.y, pos.z + brick.dim)
                    .setColor(1f, 1f, 1f, 1f).setUv(0, 1);
            buffer.addVertex(matrix, pos.x + brick.dim, pos.y, pos.z + brick.dim)
                    .setColor(1f, 1f, 1f, 1f).setUv(1, 1);
            buffer.addVertex(matrix, pos.x + brick.dim, pos.y, pos.z)
                    .setColor(1f, 1f, 1f, 1f).setUv(1, 0);
        }

        RenderSystem.disableCull();
        BufferUploader.drawWithShader(buffer.build());
        RenderSystem.enableCull();
    }
}