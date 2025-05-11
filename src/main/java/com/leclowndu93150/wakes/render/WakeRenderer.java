package com.leclowndu93150.wakes.render;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.config.enums.Resolution;
import com.leclowndu93150.wakes.debug.WakesDebugInfo;
import com.leclowndu93150.wakes.simulation.Brick;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.simulation.WakeNode;
import com.leclowndu93150.wakes.utils.LightmapWrapper;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;

@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
@OnlyIn(Dist.CLIENT)
public class WakeRenderer {
    public static Map<Resolution, WakeTexture> wakeTextures = null;

    private static void initTextures() {
        wakeTextures = Map.of(
                Resolution.EIGHT, new WakeTexture(Resolution.EIGHT.res, true),
                Resolution.SIXTEEN, new WakeTexture(Resolution.SIXTEEN.res, true),
                Resolution.THIRTYTWO, new WakeTexture(Resolution.THIRTYTWO.res, true)
        );
    }

    public static long lightmapTexure = -1;

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        if (WakesConfig.GENERAL.disableMod.get()) {
            WakesDebugInfo.quadsRendered = 0;
            return;
        }

        GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
        LightTexture lightTexture = gameRenderer.lightTexture();
        lightTexture.turnOnLightLayer();
        LightmapWrapper.updateTexture(lightTexture);

        if (wakeTextures == null) initTextures();

        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null || WakeHandler.resolutionResetScheduled) return;

        ArrayList<Brick> bricks = wakeHandler.getVisible(event.getFrustum(), Brick.class);

        Matrix4f matrix = event.getPoseStack().last().pose();
        RenderSystem.enableBlend();

        Resolution resolution = WakeHandler.resolution;
        int n = 0;
        long tRendering = System.nanoTime();
        for (var brick : bricks) {
            render(matrix, event.getCamera(), brick, wakeTextures.get(resolution));
            n++;
        }
        WakesDebugInfo.renderingTime.add(System.nanoTime() - tRendering);
        WakesDebugInfo.quadsRendered = n;
    }

    private static void render(Matrix4f matrix, Camera camera, Brick brick, WakeTexture texture) {
        if (!brick.hasPopulatedPixels) return;
        texture.loadTexture(brick.imgPtr, GlConst.GL_RGBA);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);

        Vector3f pos = brick.pos.add(camera.getPosition().reverse()).toVector3f().add(0, WakeNode.WATER_OFFSET, 0);
        buffer.addVertex(matrix, pos.x, pos.y, pos.z)
                .setUv(0, 0)
                .setColor(1f, 1f, 1f, 1f)
                .setNormal(0f, 1f, 0f);
        buffer.addVertex(matrix, pos.x, pos.y, pos.z + brick.dim)
                .setUv(0, 1)
                .setColor(1f, 1f, 1f, 1f)
                .setNormal(0f, 1f, 0f);
        buffer.addVertex(matrix, pos.x + brick.dim, pos.y, pos.z + brick.dim)
                .setUv(1, 1)
                .setColor(1f, 1f, 1f, 1f)
                .setNormal(0f, 1f, 0f);
        buffer.addVertex(matrix, pos.x + brick.dim, pos.y, pos.z)
                .setUv(1, 0)
                .setColor(1f, 1f, 1f, 1f)
                .setNormal(0f, 1f, 0f);

        BufferUploader.drawWithShader(Objects.requireNonNull(buffer.build()));
    }
}