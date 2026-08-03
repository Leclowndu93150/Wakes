package com.leclowndu93150.wakes.render;

import com.leclowndu93150.wakes.config.WakesConfig;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.leclowndu93150.wakes.simulation.Brick;
import com.leclowndu93150.wakes.simulation.QuadTree;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.simulation.WakeNode;
import com.leclowndu93150.wakes.debug.WakesDebugInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;

public class WakeRenderer {

    public static void render(SubmitNodeCollector collector, PoseStack poseStack, Frustum frustum, Vec3 cameraPos) {
        if (WakesConfig.GENERAL.disableMod.get()) {
            WakesDebugInfo.quadsRendered = 0;
            return;
        }

        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null || WakeHandler.resolutionResetScheduled) return;

        ArrayList<Brick> bricks = wakeHandler.getVisible(frustum, Brick.class);
        if (bricks.isEmpty()) return;

        int n = 0;
        long tRendering = System.nanoTime();

        ClientLevel level = Minecraft.getInstance().level;
        BlockPos.MutableBlockPos lightPos = new BlockPos.MutableBlockPos();

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

            RenderType type = brick.wakeTexture.renderType();

            Vector3f pos = brick.pos.add(cameraPos.reverse()).toVector3f().add(0, WakeNode.WATER_OFFSET, 0);
            float dim = brick.dim;

            int bx = (int) Math.floor(brick.pos.x);
            int by = (int) Math.floor(brick.pos.y);
            int bz = (int) Math.floor(brick.pos.z);
            final int light00 = lightAt(level, lightPos, bx, by, bz);
            final int light01 = lightAt(level, lightPos, bx, by, bz + (int) dim);
            final int light11 = lightAt(level, lightPos, bx + (int) dim, by, bz + (int) dim);
            final int light10 = lightAt(level, lightPos, bx + (int) dim, by, bz);

            final float px = pos.x, py = pos.y, pz = pos.z;
            final float pdim = dim;

            submitAfterTerrain(collector, poseStack, type, (pose, vc) -> {
                var m = pose.pose();
                vc.addVertex(m, px,         py, pz        ).setUv(0, 0).setColor(1f, 1f, 1f, 1f).setLight(light00).setNormal(0f, 1f, 0f);
                vc.addVertex(m, px,         py, pz + pdim).setUv(0, 1).setColor(1f, 1f, 1f, 1f).setLight(light01).setNormal(0f, 1f, 0f);
                vc.addVertex(m, px + pdim,  py, pz + pdim).setUv(1, 1).setColor(1f, 1f, 1f, 1f).setLight(light11).setNormal(0f, 1f, 0f);
                vc.addVertex(m, px + pdim,  py, pz       ).setUv(1, 0).setColor(1f, 1f, 1f, 1f).setLight(light10).setNormal(0f, 1f, 0f);
            });
            n++;
        }

        WakesDebugInfo.renderingTime.add(System.nanoTime() - tRendering);
        WakesDebugInfo.quadsRendered = n;
    }

    static void submitAfterTerrain(SubmitNodeCollector collector, PoseStack poseStack, RenderType type, SubmitNodeCollector.CustomGeometryRenderer renderer) {
        SubmitNodeCollection collection = null;
        if (collector instanceof SubmitNodeStorage storage) {
            collection = storage.order(0);
        } else if (collector instanceof SubmitNodeCollection c) {
            collection = c;
        }
        if (collection != null) {
            collection.afterTerrain.submit(new CustomFeatureRenderer.Submit(poseStack.last().copy(), type, renderer));
        } else {
            collector.submitCustomGeometry(poseStack, type, renderer);
        }
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
