package com.leclowndu93150.wakes.mixin;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.config.enums.EffectSpawningRule;
import com.leclowndu93150.wakes.duck.ProducesWake;
import com.leclowndu93150.wakes.particle.custom.SplashPlaneParticle;
import com.leclowndu93150.wakes.utils.WakesUtils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class WakeSpawnerMixin implements ProducesWake {

    @Shadow public double posX;
    @Shadow public double posY;
    @Shadow public double posZ;
    @Shadow public World world;
    @Shadow public float width;
    @Shadow public float rotationYaw;
    @Shadow public double motionX;
    @Shadow public double motionY;
    @Shadow public double motionZ;

    @Shadow public abstract AxisAlignedBB getEntityBoundingBox();
    @Shadow public abstract boolean isInWater();

    @Unique private boolean wakes$onFluidSurface = false;
    @Unique private Vec3d wakes$prevPosOnSurface = null;
    @Unique private Vec3d wakes$numericalVelocity = Vec3d.ZERO;
    @Unique private double wakes$horizontalNumericalVelocity = 0;
    @Unique private Float wakes$wakeHeightValue = null;
    @Unique private SplashPlaneParticle wakes$splashPlane;
    @Unique private boolean wakes$hasRecentlyTeleported = false;

    @Override
    public boolean wakes$onFluidSurface() {
        return this.wakes$onFluidSurface;
    }

    @Override
    public Vec3d wakes$getNumericalVelocity() {
        return this.wakes$numericalVelocity;
    }

    @Override
    public double wakes$getHorizontalVelocity() {
        return this.wakes$horizontalNumericalVelocity;
    }

    @Override
    public Vec3d wakes$getPrevPos() {
        return this.wakes$prevPosOnSurface;
    }

    @Override
    public void wakes$setPrevPos(Vec3d pos) {
        this.wakes$prevPosOnSurface = pos;
    }

    @Override
    public Float wakes$wakeHeight() {
        return this.wakes$wakeHeightValue;
    }

    @Override
    public void wakes$setWakeHeight(float h) {
        this.wakes$wakeHeightValue = h;
    }

    @Override
    public void wakes$setSplashPlane(SplashPlaneParticle particle) {
        this.wakes$splashPlane = particle;
    }

    @Override
    public void wakes$setRecentlyTeleported(boolean b) {
        this.wakes$hasRecentlyTeleported = b;
    }

    @Override
    public SplashPlaneParticle wakes$getSplashPlane() {
        return this.wakes$splashPlane;
    }

    @Inject(at = @At("HEAD"), method = "setPositionAndRotation(DDDFF)V")
    private void wakes$onSetPositionAndRotation(double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
        double dx = this.posX - x;
        double dy = this.posY - y;
        double dz = this.posZ - z;
        double distanceSq = dx * dx + dy * dy + dz * dz;
        if (distanceSq > 400) {
            this.wakes$setRecentlyTeleported(true);
            this.wakes$setPrevPos(null);
        }
    }

    @Inject(at = @At("HEAD"), method = "setLocationAndAngles(DDDFF)V")
    private void wakes$onSetLocationAndAngles(double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
        double dx = this.posX - x;
        double dy = this.posY - y;
        double dz = this.posZ - z;
        double distanceSq = dx * dx + dy * dy + dz * dz;
        if (distanceSq > 400) {
            this.wakes$setRecentlyTeleported(true);
            this.wakes$setPrevPos(null);
        }
    }

    @Inject(at = @At("HEAD"), method = "setPosition(DDD)V")
    private void wakes$onSetPosition(double x, double y, double z, CallbackInfo ci) {
        double dx = this.posX - x;
        double dy = this.posY - y;
        double dz = this.posZ - z;
        double distanceSq = dx * dx + dy * dy + dz * dz;
        if (distanceSq > 100) {
            this.wakes$setRecentlyTeleported(true);
            this.wakes$setPrevPos(null);
        }
    }

    @Unique
    private boolean wakes$checkOnFluidSurface() {
        AxisAlignedBB box = this.getEntityBoundingBox();
        double hitboxMaxY = box.maxY;

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (int y = (int) Math.floor(box.minY); y <= (int) Math.floor(hitboxMaxY); y++) {
            blockPos.setPos((int) Math.floor(this.posX), y, (int) Math.floor(this.posZ));
            IBlockState state = this.world.getBlockState(blockPos);
            if (state.getMaterial() == Material.WATER) {
                Block block = state.getBlock();
                int level;
                if (block instanceof BlockLiquid) {
                    level = state.getValue(BlockLiquid.LEVEL);
                } else if (block instanceof BlockFluidBase) {
                    level = state.getValue(BlockFluidBase.LEVEL);
                } else {
                    level = 0;
                }
                float fluidHeight = (float) blockPos.getY() + BlockLiquid.getLiquidHeightPercent(level);
                return hitboxMaxY > fluidHeight;
            }
        }
        return false;
    }

    @Inject(at = @At("TAIL"), method = "onUpdate")
    private void wakes$onTick(CallbackInfo info) {
        if (!this.world.isRemote) {
            return;
        }

        this.wakes$onFluidSurface = wakes$checkOnFluidSurface();
        Entity thisEntity = (Entity) (Object) this;
        Vec3d vel = this.wakes$calculateVelocity(thisEntity);
        this.wakes$numericalVelocity = vel;
        this.wakes$horizontalNumericalVelocity = Math.sqrt(vel.x * vel.x + vel.z * vel.z);

        if (WakesConfig.disableMod) {
            return;
        }

        if (this.wakes$onFluidSurface && !this.wakes$hasRecentlyTeleported) {
            this.wakes$wakeHeightValue = WakesUtils.getFluidLevel(this.world, thisEntity);

            Vec3d currPos = new Vec3d(thisEntity.posX, this.wakes$wakeHeightValue, thisEntity.posZ);

            this.wakes$spawnEffects(thisEntity);

            this.wakes$setPrevPos(currPos);
        } else {
            this.wakes$wakeHeightValue = null;
            this.wakes$prevPosOnSurface = null;
        }
        this.wakes$setRecentlyTeleported(false);
    }

    @Inject(at = @At("TAIL"), method = "doWaterSplashEffect")
    private void wakes$onSwimmingStart(CallbackInfo ci) {
        if (WakesConfig.disableMod) {
            return;
        }
        Entity thisEntity = (Entity) (Object) this;

        EffectSpawningRule rule = WakesUtils.getEffectRuleFromSource(thisEntity);
        if (rule.simulateWakes) {
            if (this.wakes$wakeHeightValue == null)
                this.wakes$wakeHeightValue = WakesUtils.getFluidLevel(this.world, thisEntity);
            WakesUtils.placeFallSplash(thisEntity);
        }
    }

    @Unique
    private void wakes$spawnEffects(Entity thisEntity) {
        EffectSpawningRule rule = WakesUtils.getEffectRuleFromSource(thisEntity);
        if (rule.simulateWakes) {
            WakesUtils.placeWakeTrail(thisEntity);
        }
        if (rule.renderPlanes) {
            if (this.wakes$splashPlane == null && this.wakes$horizontalNumericalVelocity > 1e-2) {
                WakesUtils.spawnSplashPlane(this.world, thisEntity);
            }
        }
    }

    @Unique
    private Vec3d wakes$calculateVelocity(Entity thisEntity) {
        if (thisEntity instanceof EntityPlayerSP) {
            return new Vec3d(thisEntity.motionX, thisEntity.motionY, thisEntity.motionZ);
        }
        if (this.wakes$prevPosOnSurface == null) {
            return Vec3d.ZERO;
        }
        return new Vec3d(this.posX - this.wakes$prevPosOnSurface.x, this.posY - this.wakes$prevPosOnSurface.y, this.posZ - this.wakes$prevPosOnSurface.z);
    }
}
