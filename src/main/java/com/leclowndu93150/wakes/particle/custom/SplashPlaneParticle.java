package com.leclowndu93150.wakes.particle.custom;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.duck.ProducesWake;
import com.leclowndu93150.wakes.simulation.SimulationNode;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.utils.WaterTintUtils;
import com.leclowndu93150.wakes.utils.WakesUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SplashPlaneParticle extends Particle {
    public Entity owner;
    public float yaw;
    public float prevYaw;

    Vec3d direction = Vec3d.ZERO;
    private int logCooldown = 0;

    private final SimulationNode simulationNode = new SimulationNode.SplashPlaneSimulation();

    public ByteBuffer imgBuffer = null;
    public int texRes;
    public boolean hasPopulatedPixels = false;

    public boolean isRenderReady = false;
    public float lerpedYaw = 0;

    private boolean alive = true;

    public SplashPlaneParticle(World world, double x, double y, double z) {
        super(world, x, y, z);
        initTexture(WakeHandler.resolution.res);
        WakeHandler.getInstance(world).ifPresent(wakeHandler -> wakeHandler.registerSplashPlane(this));
    }

    public boolean isAlive() {
        return alive;
    }

    public double getX() { return posX; }
    public double getY() { return posY; }
    public double getZ() { return posZ; }
    public double getPrevX() { return prevPosX; }
    public double getPrevY() { return prevPosY; }
    public double getPrevZ() { return prevPosZ; }

    @Override
    public void setExpired() {
        if (this.owner instanceof ProducesWake) {
            ((ProducesWake) this.owner).wakes$setSplashPlane(null);
        }
        this.owner = null;
        this.deallocTexture();
        this.alive = false;
        super.setExpired();
    }

    @Override
    public void onUpdate() {
        if (WakesConfig.disableMod || !WakesUtils.getEffectRuleFromSource(this.owner).renderPlanes) {
            this.setExpired();
            return;
        }
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.prevYaw = this.yaw;

        if (this.owner != null && this.owner instanceof ProducesWake) {
            ProducesWake wakeOwner = (ProducesWake) this.owner;
            if (this.owner.isDead || !wakeOwner.wakes$onFluidSurface() || wakeOwner.wakes$getHorizontalVelocity() < 1e-2) {
                this.setExpired();
            } else {
                this.aliveTick(wakeOwner);
            }
        } else {
            this.setExpired();
        }
    }

    private void aliveTick(ProducesWake wakeProducer) {
        Vec3d vel = new Vec3d(this.owner.motionX, this.owner.motionY, this.owner.motionZ);
        if (this.owner instanceof EntityBoat) {
            this.yaw = -this.owner.rotationYaw;
        } else {
            this.yaw = 90f - (float) (180f / Math.PI * Math.atan2(vel.z, vel.x));
        }
        double yawRad = Math.toRadians(-this.yaw);
        this.direction = new Vec3d(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3d planeOffset = direction.scale(this.owner.width + WakesConfig.splashPlaneOffset);
        Vec3d planePos = new Vec3d(this.owner.posX, this.owner.posY, this.owner.posZ).add(planeOffset);
        this.setPosition(planePos.x, wakeProducer.wakes$wakeHeight(), planePos.z);

        if (logCooldown <= 0) {
            com.leclowndu93150.wakes.WakesMod.LOGGER.info("[SPLASH_PLANE_TICK] ownerY={} wakeHeight={} planeY={} posY={}",
                String.format("%.3f", this.owner.posY),
                String.format("%.3f", wakeProducer.wakes$wakeHeight()),
                String.format("%.3f", planePos.y),
                String.format("%.3f", this.posY));
            logCooldown = 40;
        }
        logCooldown--;

        double speed = vel.length();
        if (speed / WakesConfig.maxSplashPlaneVelocity > 0.3f && WakesConfig.spawnParticles) {
            java.util.Random random = new java.util.Random();
            Vec3d particleOffset = new Vec3d(-direction.z, 0, direction.x).scale(random.nextDouble() * this.owner.width / 4);
            Vec3d particlePos = new Vec3d(this.owner.posX, this.owner.posY, this.owner.posZ).add(direction.scale(this.owner.width - 0.3));
            double particleYaw = Math.toRadians(-this.yaw + 30 * (random.nextDouble() - 0.5));
            double pitchRad = Math.toRadians(45 * random.nextDouble());
            double vScale = 1.5 * speed;
            double vx = -Math.sin(particleYaw) * Math.cos(pitchRad) * vScale;
            double vy = Math.sin(pitchRad) * vScale;
            double vz = Math.cos(particleYaw) * Math.cos(pitchRad) * vScale;
            double splashY = this.posY + 0.1;
            Minecraft.getMinecraft().effectRenderer.addEffect(
                    new SplashCloudParticle(world, particlePos.x + particleOffset.x, splashY, particlePos.z + particleOffset.z, vx, vy, vz));
            Minecraft.getMinecraft().effectRenderer.addEffect(
                    new SplashCloudParticle(world, particlePos.x - particleOffset.x, splashY, particlePos.z - particleOffset.z, vx, vy, vz));
        }

        this.simulationNode.tick((float) wakeProducer.wakes$getHorizontalVelocity(), null, null, null, null);
        populatePixels();
    }

    public void initTexture(int res) {
        int size = 4 * res * res;
        this.imgBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        this.texRes = res;
        this.hasPopulatedPixels = false;
    }

    public void deallocTexture() {
        imgBuffer = null;
    }

    public void populatePixels() {
        if (this.owner == null) return;
        BlockPos ownerPos = this.owner.getPosition();
        BlockPos fluidPos = world.getBlockState(ownerPos).getMaterial().isLiquid() ? ownerPos : ownerPos.down();
        int fluidColor = WaterTintUtils.getFluidColor(world, fluidPos);
        int light = world.getCombinedLight(ownerPos, 0);
        int skyLight = (light >> 20) & 0xF;
        int blockLight = (light >> 4) & 0xF;
        float sunBrightness = world.getSunBrightness(1.0f);
        float effectiveSkyLight = skyLight * sunBrightness;
        float brightness = Math.max(effectiveSkyLight, blockLight) / 15f;
        int b = (int) (brightness * 255);
        int lightCol = 0xFF000000 | (b << 16) | (b << 8) | b;
        float opacity = (float) WakesConfig.wakeOpacity * 0.9f;
        int res = WakeHandler.resolution.res;
        for (int r = 0; r < res; r++) {
            for (int c = 0; c < res; c++) {
                int pixelOffset = 4 * ((r * res) + c);
                imgBuffer.putInt(pixelOffset, simulationNode.getPixelColor(c, r, fluidColor, lightCol, opacity));
            }
        }
        this.hasPopulatedPixels = true;
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entityIn, float partialTicks, float rotX, float rotZ, float rotYZ, float rotXY, float rotXZ) {
        this.isRenderReady = false;
        if (!alive) return;
        if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 0 &&
                !WakesConfig.firstPersonSplashPlane &&
                this.owner instanceof EntityPlayerSP) {
            return;
        }

        float diff = this.yaw - this.prevYaw;
        if (diff > 180f) {
            diff -= 360;
        } else if (diff < -180f) {
            diff += 360;
        }

        this.lerpedYaw = (this.prevYaw + diff * partialTicks) % 360f;
        this.isRenderReady = true;
    }

    @Override
    public int getFXLayer() {
        return 3;
    }
}
