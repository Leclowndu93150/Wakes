package com.leclowndu93150.wakes.debug;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.simulation.Brick;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.simulation.WakeNode;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.awt.Color;
import java.util.Random;

@EventBusSubscriber(value = Dist.CLIENT)
public class WakeDebugRenderer {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent.AfterParticles event) {
        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null) return;

        if (WakesConfig.DEBUG.drawDebugBoxes.get()) {
            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource.BufferSource bufferSource = event.getLevelRenderer().renderBuffers.bufferSource();
            Vec3 cameraPos = event.getCamera().getPosition();

            for (var node : wakeHandler.getVisible(event.getFrustum(), WakeNode.class)) {
                DebugRenderer.renderFilledBox(poseStack, bufferSource,
                        node.toBox().move(cameraPos.reverse()),
                        1, 0, 1, 0.5f);
            }

            for (var brick : wakeHandler.getVisible(event.getFrustum(), Brick.class)) {
                Vec3 pos = brick.pos;
                AABB box = new AABB(pos.x, pos.y - (1 - WakeNode.WATER_OFFSET), pos.z, pos.x + brick.dim, pos.y, pos.z + brick.dim);
                var col = Color.getHSBColor(new Random(pos.hashCode()).nextFloat(), 1f, 1f).getRGBColorComponents(null);
                DebugRenderer.renderFilledBox(poseStack, bufferSource,
                        box.move(cameraPos.reverse()),
                        col[0], col[1], col[2], 0.5f);
            }
        }
    }
}