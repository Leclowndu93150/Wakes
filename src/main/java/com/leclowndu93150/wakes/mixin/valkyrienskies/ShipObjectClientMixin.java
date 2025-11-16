package com.leclowndu93150.wakes.mixin.valkyrienskies;

import com.leclowndu93150.wakes.compat.valkyrienskies.DynamicWakeSize;
import com.leclowndu93150.wakes.compat.valkyrienskies.VSUtils;
import com.leclowndu93150.wakes.compat.valkyrienskies.ValkyrienSkiesCompat;
import com.leclowndu93150.wakes.duck.ProducesWake;
import com.leclowndu93150.wakes.particle.custom.SplashPlaneParticle;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.impl.game.ships.ShipObjectClient;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

@Mixin(value = ShipObjectClient.class, remap = false)
public abstract class ShipObjectClientMixin implements ProducesWake, DynamicWakeSize {
    @Unique
    private Vec3 prevPosOnSurface;

    @Unique
    private float wakeWidth = 0;

    @Unique
    private Vec3 offset = Vec3.ZERO;
    
    @Unique
    private SplashPlaneParticle splashPlane;

    @Override
    public boolean wakes$onFluidSurface() {
        return true;
    }

    @Override
    public Float wakes$wakeHeight() {
        return (float) ValkyrienSkiesCompat.getSeaLevel();
    }
    
    @Override
    public void wakes$setWakeHeight(float h) {
    }

    @Override
    public Vec3 wakes$getPrevPos() {
        return this.prevPosOnSurface == null ? null : new Vec3(this.prevPosOnSurface.x, this.prevPosOnSurface.y, this.prevPosOnSurface.z);
    }

    @Override
    public float getWidth() {
        return this.wakeWidth;
    }

    @Override
    public void setWidth(float width) {
        this.wakeWidth = width;
    }

    @Override
    public Vec3 getPos() {
        return VSUtils.getCentre(((Ship) this).getWorldAABB()).add(offset);
    }

    @Override
    public void setOffset(Vector3d vec) {
        Ship ship = (Ship)(Object)this;
        Quaterniondc mat = ship.getTransform().getShipToWorldRotation();
        this.offset = VectorConversionsMCKt.toMinecraft(vec.rotate(mat));
    }

    @Override
    public void wakes$setPrevPos(Vec3 pos) {
        this.prevPosOnSurface = pos;
    }

    @Override
    public Vec3 wakes$getNumericalVelocity() {
        return VectorConversionsMCKt.toMinecraft(((Ship) this).getVelocity());
    }

    @Override
    public double wakes$getHorizontalVelocity() {
        Vector3dc velocityVector =((Ship) this).getVelocity();
        Vector3dc horizontalVelocityVector = new Vector3d(velocityVector.x(), 0, velocityVector.z());
        return horizontalVelocityVector.length();
    }

    @Override
    public void wakes$setSplashPlane(SplashPlaneParticle particle) {
        this.splashPlane = particle;
    }
    
    @Override
    public void wakes$setRecentlyTeleported(boolean b) {
    }
    
    @Override
    public SplashPlaneParticle wakes$getSplashPlane() {
        return this.splashPlane;
    }
}
