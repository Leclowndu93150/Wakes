package com.leclowndu93150.wakes.debug;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.simulation.Brick;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.simulation.WakeNode;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.awt.*;
import java.util.Random;

@EventBusSubscriber(value = Dist.CLIENT)
public class WakeDebugRenderer {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null) return;

        if (WakesConfig.DEBUG.drawDebugBoxes.get()) {
            CameraRenderState camera = event.getLevelRenderState().cameraRenderState;

            for (var node : wakeHandler.getVisible(camera.cullFrustum, WakeNode.class)) {
                AABB box = node.toBox();
                Gizmos.cuboid(box, GizmoStyle.fill(ARGB.colorFromFloat(0.5f, 1f, 0f, 1f)));
            }

            for (var brick : wakeHandler.getVisible(camera.cullFrustum, Brick.class)) {
                Vec3 pos = brick.pos;
                AABB box = new AABB(pos.x, pos.y - (1 - WakeNode.WATER_OFFSET), pos.z, pos.x + brick.dim, pos.y, pos.z + brick.dim);
                var col = Color.getHSBColor(new Random(pos.hashCode()).nextFloat(), 1f, 1f).getRGBColorComponents(null);
                Gizmos.cuboid(box, GizmoStyle.fill(ARGB.colorFromFloat(0.5f, col[0], col[1], col[2])));
            }
        }
    }
}
