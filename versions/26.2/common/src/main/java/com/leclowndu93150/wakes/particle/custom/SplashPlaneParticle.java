package com.leclowndu93150.wakes.particle.custom;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.duck.ProducesWake;
import com.leclowndu93150.wakes.particle.ModParticles;
import com.leclowndu93150.wakes.particle.WithOwnerParticleType;
import com.leclowndu93150.wakes.render.WakeColor;
import com.leclowndu93150.wakes.render.WakeTexture;
import com.leclowndu93150.wakes.simulation.SimulationNode;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.utils.WakesUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.util.Random;

public class SplashPlaneParticle extends Particle {
    public Entity owner;
    float yaw;
    float prevYaw;

    Vec3 direction = Vec3.ZERO;

    private final SimulationNode simulationNode = new SimulationNode.SplashPlaneSimulation();

    public long imgPtr = -1;
    public int texRes;
    public boolean pixelsDirty = false;
    public WakeTexture wakeTexture = null;
    private int[] palette = null;

    public boolean isRenderReady = false;
    public float lerpedYaw = 0;

    protected SplashPlaneParticle(ClientLevel world, double x, double y, double z) {
        super(world, x, y, z);
        initTexture(WakeHandler.resolution.res);
        WakeHandler.getInstance(world).ifPresent(wakeHandler -> wakeHandler.registerSplashPlane(this));
    }

    @Override
    public void remove() {
        if (this.owner instanceof ProducesWake wakeOwner) {
            wakeOwner.wakes$setSplashPlane(null);
        }
        this.owner = null;
        this.deallocTexture();
        super.remove();
    }

    @Override
    public void tick() {
        if (WakesConfig.GENERAL.disableMod.get() || !WakesUtils.getEffectRuleFromSource(this.owner).renderPlanes) {
            this.remove();
        }
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.prevYaw = this.yaw;

        if (this.owner instanceof ProducesWake wakeOwner) {
            if (this.owner.isRemoved() || !wakeOwner.wakes$onFluidSurface() || wakeOwner.wakes$getHorizontalVelocity() < 1e-2) {
                this.remove();
            } else {
                this.aliveTick(wakeOwner);
            }
        } else {
            this.remove();
        }
        updateRenderReady();
    }

    private void aliveTick(ProducesWake wakeProducer) {
        Vec3 vel = this.owner.getDeltaMovement();
        if (this.owner instanceof AbstractBoat) {
            this.yaw = -this.owner.getYRot();
        } else {
            this.yaw = 90f - (float) (180f / Math.PI * Math.atan2(vel.z, vel.x));
        }
        this.direction = Vec3.directionFromRotation(0, -this.yaw);
        Vec3 planeOffset = direction.scale(this.owner.getBbWidth() + WakesConfig.APPEARANCE.splashPlaneOffset.get());
        Vec3 planePos = this.owner.position().add(planeOffset);
        this.setPos(planePos.x, wakeProducer.wakes$wakeHeight(), planePos.z);

        if (vel.length() / WakesConfig.APPEARANCE.maxSplashPlaneVelocity.get() > 0.3f && WakesConfig.APPEARANCE.spawnParticles.get()) {
            Random random = new Random();
            Vec3 particleOffset = new Vec3(-direction.z, 0, direction.x).scale(random.nextDouble() * this.owner.getBbWidth() / 4);
            Vec3 particlePos = this.owner.position().add(direction.scale(this.owner.getBbWidth() - 0.3));
            Vec3 particleVelocity = Vec3.directionFromRotation((float) (45 * random.nextDouble()), (float) (-this.yaw + 30 * (random.nextDouble() - 0.5f))).scale(1.5 * vel.length());
            this.level.addParticle(ModParticles.SPLASH_CLOUD.get(), particlePos.x + particleOffset.x, this.y, particlePos.z + particleOffset.z, particleVelocity.x, particleVelocity.y, particleVelocity.z);
            this.level.addParticle(ModParticles.SPLASH_CLOUD.get(), particlePos.x - particleOffset.x, this.y, particlePos.z - particleOffset.z, particleVelocity.x, particleVelocity.y, particleVelocity.z);
        }

        this.simulationNode.tick((float) wakeProducer.wakes$getHorizontalVelocity(), null, null, null, null);
        populatePixels();
    }

    public void initTexture(int res) {
        long size = 4L * res * res;
        if (imgPtr == -1) {
            imgPtr = MemoryUtil.nmemAlloc(size);
        } else {
            imgPtr = MemoryUtil.nmemRealloc(imgPtr, size);
        }

        this.texRes = res;
        this.pixelsDirty = false;
    }

    public void deallocTexture() {
        if (imgPtr != -1) {
            MemoryUtil.nmemFree(imgPtr);
            imgPtr = -1;
        }
        if (wakeTexture != null) {
            wakeTexture.destroy();
            wakeTexture = null;
        }
    }

    public void populatePixels() {
        if (imgPtr == -1) return;
        int fluidColor = BiomeColors.getAverageWaterColor(level, this.owner.blockPosition());
        int lightCol = 0xFFFFFFFF;
        float opacity = WakesConfig.APPEARANCE.wakeOpacity.get().floatValue() * 0.9f;
        int res = WakeHandler.resolution.res;
        if (WakesConfig.DEBUG.debugColors.get()) {
            for (int r = 0; r < res; r++) {
                for (int c = 0; c < res; c++) {
                    long pixelOffset = 4L * (((long) r * res) + c);
                    MemoryUtil.memPutInt(imgPtr + pixelOffset, simulationNode.getPixelColor(c, r, fluidColor, lightCol, opacity));
                }
            }
        } else {
            WakeColor.updateCaches();
            if (palette == null || palette.length != WakeColor.paletteSize()) {
                palette = new int[WakeColor.paletteSize()];
            }
            WakeColor.computePalette(palette, fluidColor, lightCol, opacity);
            float[][][] u = simulationNode.u;
            for (int r = 0; r < res; r++) {
                float[] u0 = u[0][r + 1];
                float[] u1 = u[1][r + 1];
                float[] u2 = u[2][r + 1];
                long rowPtr = imgPtr + 4L * ((long) r * res);
                for (int c = 0; c < res; c++) {
                    float waveEqAvg = (u0[c + 1] + u1[c + 1] + u2[c + 1]) / 3;
                    MemoryUtil.memPutInt(rowPtr + 4L * c, palette[WakeColor.paletteIndex(waveEqAvg)]);
                }
            }
        }
        this.pixelsDirty = true;
    }

    public void updateRenderReady() {
        this.isRenderReady = false;
        if (this.removed) return;
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson() &&
                !WakesConfig.APPEARANCE.firstPersonSplashPlane.get() &&
                this.owner instanceof LocalPlayer) {
            return;
        }
        this.isRenderReady = true;
    }

    public void translateMatrix(Vec3 cameraPos, PoseStack matrices) {
        float tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);

        float diff = this.yaw - this.prevYaw;
        if (diff > 180f) diff -= 360;
        else if (diff < -180f) diff += 360;
        this.lerpedYaw = (this.prevYaw + diff * tickDelta) % 360f;
        float x = (float) (Mth.lerp(tickDelta, this.xo, this.x) - cameraPos.x());
        float y = (float) (Mth.lerp(tickDelta, this.yo, this.y) - cameraPos.y());
        float z = (float) (Mth.lerp(tickDelta, this.zo, this.z) - cameraPos.z());

        matrices.translate(x, y, z);
    }

    public Vec3 getPos() {
        return new Vec3(x, y, z);
    }

    @Override
    public ParticleRenderType getGroup() {
        return ParticleRenderType.NO_RENDER;
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {

        public Factory(SpriteSet spriteSet) {
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientLevel world, double x, double y, double z, double velX, double velY, double velZ, net.minecraft.util.RandomSource random) {
            SplashPlaneParticle splashPlane = new SplashPlaneParticle(world, x, y, z);
            if (parameters instanceof WithOwnerParticleType type) {
                splashPlane.owner = type.owner;
                splashPlane.yaw = splashPlane.prevYaw = type.owner.getYRot();
                ((ProducesWake) splashPlane.owner).wakes$setSplashPlane(splashPlane);
            }
            return splashPlane;
        }
    }
}
