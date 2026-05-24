package com.leclowndu93150.wakes.compat.alekiships;

import com.alekiponi.alekiships.common.entity.vehicle.AbstractAlekiBoatEntity;
import com.alekiponi.alekiships.common.entity.vehicle.RowboatEntity;
import com.alekiponi.alekiships.common.entity.vehicle.SloopEntity;
import com.leclowndu93150.wakes.mixin.AlekiBoatAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.loading.FMLLoader;

public class AlekiShipsCompat {

    private static Boolean alekiShipsLoaded = null;

    public static boolean isAlekiShipsLoaded() {
        if (alekiShipsLoaded == null) {
            alekiShipsLoaded = FMLLoader.getLoadingModList().getModFileById("alekiships") != null;
        }
        return alekiShipsLoaded;
    }

    public static boolean isAlekiShipsBoat(Object entity) {
        if (!isAlekiShipsLoaded()) return false;
        return entity instanceof AbstractAlekiBoatEntity;
    }

    public static PaddleInfo getPaddleInfo(AbstractAlekiBoatEntity boat, float partialTicks, float wakeHeight) {
        if (boat instanceof RowboatEntity rowboat) {
            return getRowboatPaddleInfo(rowboat, partialTicks, wakeHeight);
        } else if (boat instanceof SloopEntity sloop) {
            return getSloopPaddleInfo(sloop, partialTicks, wakeHeight);
        }
        return new PaddleInfo(new Vec3[0], new boolean[0]);
    }

    private static PaddleInfo getRowboatPaddleInfo(RowboatEntity rowboat, float partialTicks, float wakeHeight) {
        if (rowboat.getOars() == RowboatEntity.Oars.ZERO) {
            return new PaddleInfo(new Vec3[0], new boolean[0]);
        }

        int oarCount = rowboat.getOars() == RowboatEntity.Oars.TWO ? 2 : 1;
        Vec3[] paddlePositions = new Vec3[oarCount];
        boolean[] inWater = new boolean[oarCount];

        float yaw = rowboat.getYRot();
        float yawRad = (float) Math.toRadians(yaw);

        for (int i = 0; i < oarCount; i++) {
            if (!rowboat.getPaddleState(i)) {
                paddlePositions[i] = new Vec3(rowboat.getX(), rowboat.getY(), rowboat.getZ());
                inWater[i] = false;
                continue;
            }

            float rowingTime = rowboat.getRowingTime(i, partialTicks);
            
            Vec3 viewVector = rowboat.getViewVector(1.0F);
            
            float[] paddlePositionsArray = ((AlekiBoatAccessor) rowboat).wakes$getPaddlePositions();
            float paddlePhase = (float) (paddlePositionsArray[i] % (Math.PI * 2));
            
            float oarPivotOffset = 14.5f / 16.0f;
            float oarBladeEndOffset = 35.875f / 16.0f;
            
            float paddleSwing = (Mth.sin(-rowingTime) + 1.0F) / 2.0F;
            float yRotation = Mth.clampedLerp(0.7853981634f, -0.7853981634f, (Mth.sin(-rowingTime + 1.0F) + 1.0F) / 2.0F);
            if (i == 1) yRotation = -yRotation;
            
            float totalDistance = oarPivotOffset + oarBladeEndOffset;
            
            double perpX = i == 1 ? -viewVector.z : viewVector.z;
            double perpZ = i == 1 ? viewVector.x : -viewVector.x;
            
            double rotatedX = perpX * Mth.cos(yRotation) - viewVector.x * Mth.sin(yRotation);
            double rotatedZ = perpZ * Mth.cos(yRotation) - viewVector.z * Mth.sin(yRotation);
            
            float worldX = (float) (rowboat.getX() + rotatedX * totalDistance);
            float worldY = wakeHeight;
            float worldZ = (float) (rowboat.getZ() + rotatedZ * totalDistance);

            paddlePositions[i] = new Vec3(worldX, worldY, worldZ);
            
            double PADDLE_SPEED = Math.PI / 8.0;
            double PADDLE_SOUND_TIME = Math.PI / 4.0;
            boolean isPaddleInWaterPhase = paddlePhase >= (PADDLE_SPEED / 2.0) && 
                                          paddlePhase <= (PADDLE_SOUND_TIME + PADDLE_SPEED);
            
            inWater[i] = isPaddleInWaterPhase;
        }

        return new PaddleInfo(paddlePositions, inWater);
    }

    private static PaddleInfo getSloopPaddleInfo(SloopEntity sloop, float partialTicks, float wakeHeight) {
        return new PaddleInfo(new Vec3[0], new boolean[0]);
    }

    public static BoatDimensions getBoatDimensions(AbstractAlekiBoatEntity boat) {
        if (boat instanceof RowboatEntity) {
            return new BoatDimensions(
                1.8f,
                -1.2f,
                0.8f
            );
        } else if (boat instanceof SloopEntity) {
            return new BoatDimensions(
                3.5f,
                -3.0f,
                1.5f
            );
        }
        
        float bbWidth = boat.getBbWidth();
        float bbHeight = boat.getBbHeight();
        return new BoatDimensions(
            bbWidth * 0.4f,
            -bbWidth * 0.4f,
            bbWidth * 0.5f
        );
    }

    public static record PaddleInfo(Vec3[] positions, boolean[] inWater) {}

    public static record BoatDimensions(float front, float back, float width) {}
}
