package com.leclowndu93150.wakes.particle;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;

public class WithOwnerParticleType extends SimpleParticleType {
    public Entity owner;

    protected WithOwnerParticleType(boolean alwaysShow) {
        super(alwaysShow);
    }

    public WithOwnerParticleType withOwner(Entity owner) {
        this.owner = owner;
        return this;
    }
}
