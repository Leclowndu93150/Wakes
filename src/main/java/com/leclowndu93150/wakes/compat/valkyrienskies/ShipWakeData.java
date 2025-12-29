package com.leclowndu93150.wakes.compat.valkyrienskies;

import com.leclowndu93150.wakes.particle.custom.SplashPlaneParticle;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

/**
 * Holds per-ship wake state data.
 */
public class ShipWakeData {
    private final Ship ship;
    private Vec3 prevPosOnSurface;
    private float wakeWidth = 0;
    private Vec3 offset = Vec3.ZERO;
    private SplashPlaneParticle splashPlane;

    public ShipWakeData(Ship ship) {
        this.ship = ship;
    }

    public boolean onFluidSurface() {
        return true;
    }

    public float wakeHeight() {
        return (float) ValkyrienSkiesCompat.getSeaLevel();
    }

    public Vec3 getPrevPos() {
        return this.prevPosOnSurface == null ? null : new Vec3(this.prevPosOnSurface.x, this.prevPosOnSurface.y, this.prevPosOnSurface.z);
    }

    public void setPrevPos(Vec3 pos) {
        this.prevPosOnSurface = pos;
    }

    public Vec3 getNumericalVelocity() {
        return VectorConversionsMCKt.toMinecraft(ship.getVelocity());
    }

    public double getHorizontalVelocity() {
        Vector3dc velocityVector = ship.getVelocity();
        Vector3dc horizontalVelocityVector = new Vector3d(velocityVector.x(), 0, velocityVector.z());
        return horizontalVelocityVector.length();
    }

    public void setSplashPlane(SplashPlaneParticle particle) {
        this.splashPlane = particle;
    }

    public SplashPlaneParticle getSplashPlane() {
        return this.splashPlane;
    }

    public float getWidth() {
        return this.wakeWidth;
    }

    public void setWidth(float width) {
        this.wakeWidth = width;
    }

    public Vec3 getPos() {
        return VSUtils.getCentre(ship.getWorldAABB()).add(offset);
    }

    public void setOffset(Vector3d vec) {
        Quaterniondc mat = ship.getTransform().getShipToWorldRotation();
        this.offset = VectorConversionsMCKt.toMinecraft(vec.rotate(mat));
    }
}
