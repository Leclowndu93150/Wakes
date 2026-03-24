package com.leclowndu93150.wakes.particle.custom;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.particle.WithOwnerParticleType;
import com.leclowndu93150.wakes.simulation.WakeNode;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class SplashCloudParticle extends SingleQuadParticle {
    Entity owner;
    final double offset;
    final boolean isFromPaddles;

    public SplashCloudParticle(ClientLevel world, double x, double y, double z, TextureAtlasSprite sprite, double velocityX, double velocityY, double velocityZ) {
        super(world, x, y, z, sprite);
        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;

        this.xo = x;
        this.yo = y;
        this.zo = z;

        this.lifetime = (int) (WakeNode.maxAge * 1.5);

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

        var fluidState = level.getFluidState(new BlockPos((int) this.x, (int) this.y, (int) this.z));
        if (fluidState.isSource() && WakesConfig.getFluidWhitelist().contains(fluidState.getType())) {
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
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.bySprite(this.sprite);
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Factory(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, net.minecraft.util.RandomSource random) {
            TextureAtlasSprite sprite = this.sprites.get(world.getRandom());
            SplashCloudParticle cloud = new SplashCloudParticle(world, x, y, z, sprite, velocityX, velocityY, velocityZ);
            if (parameters instanceof WithOwnerParticleType type) {
                cloud.owner = type.owner;
            }
            return cloud;
        }
    }
}
