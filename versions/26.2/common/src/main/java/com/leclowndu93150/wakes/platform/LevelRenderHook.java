package com.leclowndu93150.wakes.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;

@FunctionalInterface
public interface LevelRenderHook {
    void run(SubmitNodeCollector collector, PoseStack poseStack, Frustum frustum, Vec3 cameraPos);
}
