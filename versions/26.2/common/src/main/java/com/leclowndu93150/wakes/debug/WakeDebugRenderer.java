package com.leclowndu93150.wakes.debug;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.simulation.Brick;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.simulation.WakeNode;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.Random;

public class WakeDebugRenderer {

    public static void render(SubmitNodeCollector collector, PoseStack poseStack, Frustum frustum, Vec3 cameraPos) {
        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null) return;

        if (WakesConfig.DEBUG.drawDebugBoxes.get()) {
            for (var node : wakeHandler.getVisible(frustum, WakeNode.class)) {
                AABB box = node.toBox();
                Gizmos.cuboid(box, GizmoStyle.fill(ARGB.colorFromFloat(0.5f, 1f, 0f, 1f)));
            }

            for (var brick : wakeHandler.getVisible(frustum, Brick.class)) {
                Vec3 pos = brick.pos;
                AABB box = new AABB(pos.x, pos.y - (1 - WakeNode.WATER_OFFSET), pos.z, pos.x + brick.dim, pos.y, pos.z + brick.dim);
                var col = Color.getHSBColor(new Random(pos.hashCode()).nextFloat(), 1f, 1f).getRGBColorComponents(null);
                Gizmos.cuboid(box, GizmoStyle.fill(ARGB.colorFromFloat(0.5f, col[0], col[1], col[2])));
            }
        }
    }
}
