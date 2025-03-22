package com.leclowndu93150.wakes.particle.custom;

import com.leclowndu93150.wakes.particle.WithOwnerParticleType;
import com.leclowndu93150.wakes.simulation.WakeNode;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;

public class SplashCloudParticle extends TextureSheetParticle {
    Entity owner;
    final double offset;
    final boolean isFromPaddles;

    public SplashCloudParticle(ClientLevel world, double x, double y, double z, SpriteSet sprites, double velocityX, double velocityY, double velocityZ) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;

        this.xo = x;
        this.yo = y;
        this.zo = z;

        this.lifetime = (int) (WakeNode.maxAge * 1.5);
        this.setSprite(sprites.get(world.random));

        this.offset = velocityX;
        this.isFromPaddles = velocityX == 0;
        this.quadSize = isFromPaddles ? quadSize * 2 : 0.3f;
    }

    @Override
    public void tick() {
        this.age++;
        if (this.isFromPaddles) {
            if (this.age > lifetime) {
                this.remove();
                return;
            }
            this.alpha = 1f - (float) this.age / this.lifetime;
            return;
        } else {
            if (this.age > lifetime / 3) {
                this.remove();
                return;
            }
            this.alpha = 1f - (float) this.age / (this.lifetime / 3f);
        }
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (level.getFluidState(new BlockPos((int) this.x, (int) this.y, (int) this.z)).is(Fluids.WATER)) {
            this.yd = 0.1;
            this.xd *= 0.92;
            this.yd *= 0.92;
            this.zd *= 0.92;
        } else {
            this.yd -= 0.05;

            this.xd *= 0.95;
            this.yd *= 0.95;
            this.zd *= 0.95;
        }

        this.x += xd;
        this.y += yd;
        this.z += zd;
        this.setPos(this.x, this.y, this.z);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Factory(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            SplashCloudParticle cloud = new SplashCloudParticle(world, x, y, z, this.sprites, velocityX, velocityY, velocityZ);
            if (parameters instanceof WithOwnerParticleType type) {
                cloud.owner = type.owner;
            }
            return cloud;
        }
    }
}
