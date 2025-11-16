package com.leclowndu93150.wakes.compat.valkyrienskies;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public interface DynamicWakeSize {
    float getWidth();

    void setWidth(float width);

    Vec3 getPos();

    void setOffset(Vector3d pos);
}
