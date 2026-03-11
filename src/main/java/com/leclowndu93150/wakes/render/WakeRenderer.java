package com.leclowndu93150.wakes.render;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.config.enums.Resolution;
import com.leclowndu93150.wakes.debug.WakesDebugInfo;
import com.leclowndu93150.wakes.simulation.Brick;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.simulation.WakeNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.*;

public class WakeRenderer {
    public static Map<Resolution, WakeTexture> wakeTextures = null;

    private static void initTextures() {
        wakeTextures = new HashMap<>();
        wakeTextures.put(Resolution.EIGHT, new WakeTexture(Resolution.EIGHT.res, true));
        wakeTextures.put(Resolution.SIXTEEN, new WakeTexture(Resolution.SIXTEEN.res, true));
        wakeTextures.put(Resolution.THIRTYTWO, new WakeTexture(Resolution.THIRTYTWO.res, true));
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (WakesConfig.disableMod) {
            WakesDebugInfo.quadsRendered = 0;
            return;
        }

        if (wakeTextures == null) initTextures();

        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null || WakeHandler.resolutionResetScheduled) return;

        Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();
        if (viewer == null) return;

        double cx = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * event.getPartialTicks();
        double cy = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * event.getPartialTicks();
        double cz = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * event.getPartialTicks();

        Frustum frustum = new Frustum();
        frustum.setPosition(cx, cy, cz);

        ArrayList<Brick> bricks = wakeHandler.getVisible(frustum, Brick.class);

        Resolution resolution = WakeHandler.resolution;
        int n = 0;
        long tRendering = System.nanoTime();

        GlStateManager.pushAttrib();
        GlStateManager.pushMatrix();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.enableTexture2D();
        GlStateManager.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableLighting();
        GlStateManager.disableAlpha();
        GlStateManager.depthMask(false);
        GL11.glEnable(GL11.GL_FOG);

        for (Brick brick : bricks) {
            render(cx, cy, cz, brick, wakeTextures.get(resolution));
            n++;
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableAlpha();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.enableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.popMatrix();
        GlStateManager.popAttrib();

        WakesDebugInfo.renderingTime.add(System.nanoTime() - tRendering);
        WakesDebugInfo.quadsRendered = n;
    }

    private static void render(double cx, double cy, double cz, Brick brick, WakeTexture texture) {
        if (!brick.hasPopulatedPixels) return;
        if (brick.imgBuffer == null) return;

        texture.loadTexture(brick.imgBuffer);

        GlStateManager.disableCull();

        float px = (float) (brick.pos.x - cx);
        float py = (float) (brick.pos.y - cy + WakeNode.WATER_OFFSET);
        float pz = (float) (brick.pos.z - cz);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        buffer.pos(px, py, pz)
                .tex(0, 0).endVertex();
        buffer.pos(px, py, pz + brick.dim)
                .tex(0, 1).endVertex();
        buffer.pos(px + brick.dim, py, pz + brick.dim)
                .tex(1, 1).endVertex();
        buffer.pos(px + brick.dim, py, pz)
                .tex(1, 0).endVertex();

        tessellator.draw();
        GlStateManager.enableCull();
    }
}
