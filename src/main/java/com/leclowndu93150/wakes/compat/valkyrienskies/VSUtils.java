package com.leclowndu93150.wakes.compat.valkyrienskies;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.primitives.AABBdc;
import org.joml.primitives.AABBic;

public class VSUtils {
    public static Vec3 getCentre(AABBdc aabb) {
        double centreX = (aabb.maxX() + aabb.minX())/2;
        double centreZ = (aabb.maxZ() + aabb.minZ())/2;
        return new Vec3(centreX, aabb.minY(), centreZ);
    }
    
    public static Vec3 getCentreD(AABBdc aabb) {
        double centreX = (aabb.maxX() + aabb.minX())/2;
        double centreY = (aabb.maxY() + aabb.minY())/2;
        double centreZ = (aabb.maxZ() + aabb.minZ())/2;
        return new Vec3(centreX, centreY, centreZ);
    }

    public static Vector3d getCentre(AABBic aabb) {
        double centreX = (double) (aabb.maxX() + aabb.minX()) /2;
        double centreZ = (double) (aabb.maxZ() + aabb.minZ()) /2;
        return new Vector3d(centreX, aabb.minY(), centreZ);
    }

    public static double getYaw(Quaterniondc quaternion) {
        double w = quaternion.w();
        double x = quaternion.x();
        double y = quaternion.y();
        double z = quaternion.z();

        return -Math.atan2(2.0 * (w * y + x * z), 1.0 - 2.0 * (y * y + z * z));
    }

    public static Direction approximateDirection(Double degrees) {
        double y = degrees + 45d + 720d;
        double reduced = y % 360d;

        if (reduced >= 0 && reduced < 90) {
            return Direction.NORTH;
        } else if (reduced >= 90 && reduced < 180) {
            return Direction.EAST;
        } else if (reduced >= 180 && reduced < 270) {
            return Direction.SOUTH;
        } else if (reduced >= 270 && reduced < 360) {
            return Direction.WEST;
        } else {
            System.out.println("something went wrong");
            return Direction.NORTH;
        }
    }

    public static Vector3d averageVec(Vec3 vec1, Vec3 vec2) {
        double avgX = (vec1.x() + vec2.x()) / 2d;
        double avgY = (vec1.y() + vec2.y()) / 2d;
        double avgZ = (vec1.z() + vec2.z()) / 2d;

        return new Vector3d(avgX, avgY, avgZ);
    }
}
