package com.leclowndu93150.wakes.simulation;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.utils.WakesUtils;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WakeNode {
    public final SimulationNode simulationNode;

    public final int x;
    public final int y;
    public final int z;
    public static final float WATER_OFFSET = 8 / 9f;

    public WakeNode NORTH = null;
    public WakeNode EAST = null;
    public WakeNode SOUTH = null;
    public WakeNode WEST = null;

    // TODO MAKE DISAPPEARANCE DEPENDENT ON WAVE VALUES INSTEAD OF AGE/TIME (MAYBE)
    public static int maxAge = 30;
    public int age = 0;
    private boolean dead = false;

    public float t = 0;
    public int floodLevel;

    private WakeNode(int x, int y, int z, int floodLevel) {
        this.simulationNode = new SimulationNode.WakeSimulation();
        this.x = x;
        this.y = y;
        this.z = z;
        this.floodLevel = floodLevel;
    }

    private WakeNode(long pos, int y) {
        this.simulationNode = new SimulationNode.WakeSimulation();
        int[] xz = WakesUtils.longAsPos(pos);
        this.x = xz[0];
        this.y = y;
        this.z = xz[1];
        this.floodLevel = WakesConfig.DEBUG.floodFillDistance.get();
    }

    public SimulationNode getSimulationNode(WakeNode neighboringNode) {
        if (neighboringNode == null) return null;
        return neighboringNode.simulationNode;
    }

    public boolean tick(WakeHandler wakeHandler) {
        if (this.isDead()) return false;
        if (this.age++ >= WakeNode.maxAge) {
            this.markDead();
            return false;
        }
        this.t = this.age / (float) WakeNode.maxAge;

        this.simulationNode.tick(
                null,
                getSimulationNode(this.NORTH),
                getSimulationNode(this.SOUTH),
                getSimulationNode(this.EAST),
                getSimulationNode(this.WEST)
        );

        floodFill(wakeHandler);
        return true;
    }

    public void floodFill(WakeHandler wakeHandler) {
        if (floodLevel > 0 && this.age > WakesConfig.DEBUG.floodFillTickDelay.get()) {
            if (this.NORTH == null) {
                wakeHandler.insert(new WakeNode(this.x, this.y, this.z - 1, floodLevel - 1));
            } else {
                this.NORTH.updateFloodLevel(floodLevel - 1);
            }
            if (this.EAST == null) {
                wakeHandler.insert(new WakeNode(this.x + 1, this.y, this.z, floodLevel - 1));
            } else {
                this.EAST.updateFloodLevel(floodLevel - 1);
            }
            if (this.SOUTH == null) {
                wakeHandler.insert(new WakeNode(this.x, this.y, this.z + 1, floodLevel - 1));
            } else {
                this.SOUTH.updateFloodLevel(floodLevel - 1);
            }
            if (this.WEST == null) {
                wakeHandler.insert(new WakeNode(this.x - 1, this.y, this.z, floodLevel - 1));
            } else {
                this.WEST.updateFloodLevel(floodLevel - 1);
            }
            floodLevel = 0;
            // TODO IF BLOCK IS BROKEN (AND WATER APPEARS IN ITS STEAD) RETRY FLOOD FILL
        }
    }

    public void updateAdjacency(WakeNode node) {
        if (node.x == this.x && node.z == this.z - 1) {
            this.NORTH = node;
            node.SOUTH = this;
            return;
        }
        if (node.x == this.x + 1 && node.z == this.z) {
            this.EAST = node;
            node.WEST = this;
            return;
        }
        if (node.x == this.x && node.z == this.z + 1) {
            this.SOUTH = node;
            node.NORTH = this;
            return;
        }
        if (node.x == this.x - 1 && node.z == this.z) {
            this.WEST = node;
            node.EAST = this;
        }
    }

    public void updateFloodLevel(int newLevel) {
        this.age = 0;
        if (newLevel > this.floodLevel) {
            this.floodLevel = newLevel;
        }
    }

    public boolean validPos(Level world) {
        FluidState fluidState = world.getFluidState(this.blockPos());
        FluidState fluidStateAbove = world.getFluidState(this.blockPos().above());
        if (fluidState.is(Fluids.WATER) && fluidStateAbove.isEmpty()) {
            return fluidState.isSource();
        }
        return false;
    }

    public AABB toBox() {
        return new AABB(this.x, this.y, this.z, this.x + 1, this.y + (1 - WakeNode.WATER_OFFSET), this.z + 1);
    }

    public void revive(WakeNode node) {
        this.age = 0;
        this.floodLevel = WakesConfig.DEBUG.floodFillDistance.get();
        this.simulationNode.initialValues = node.simulationNode.initialValues;
    }

    public void markDead() {
        this.dead = true;
    }

    public boolean isDead() {
        return this.dead;
    }

    public BlockPos blockPos() {
        return new BlockPos(this.x, this.y, this.z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WakeNode wakeNode = (WakeNode) o;
        return x == wakeNode.x && z == wakeNode.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    @Override
    public String toString() {
        return String.format("WakeNode{%d, %d, %d}", x, y, z);
    }
    public static class Factory {
        public static Set<WakeNode> splashNodes(Entity entity, int y) {
            int res = WakeHandler.resolution.res;
            int w = (int) (0.8 * entity.getBbWidth() * res / 2);
            int x = (int) (entity.getX() * res);
            int z = (int) (entity.getZ() * res);

            ArrayList<Long> pixelsAffected = new ArrayList<>();
            for (int i = -w; i < w; i++) {
                for (int j = -w; j < w; j++) {
                    if (i*i + j*j < w*w) {
                        pixelsAffected.add(WakesUtils.posAsLong(x + i, z + j));
                    }
                }
            }
            return pixelsToNodes(pixelsAffected, y, WakesConfig.GENERAL.splashStrength.get(), Math.abs(entity.getDeltaMovement().y));
        }

        public static Set<WakeNode> rowingNodes(Boat boat, int y) {
            Set<WakeNode> nodesAffected = new HashSet<>();
            double velocity = boat.getDeltaMovement().horizontalDistance();
            for (int i = 0; i < 2; i++) {
                if (boat.getPaddleState(i)) {
                    double phase = boat.paddlePositions[i] % (2*Math.PI);
                    if (Boat.PADDLE_SPEED / 2 <= phase && phase <= Boat.PADDLE_SOUND_TIME + Boat.PADDLE_SPEED) {
                        Vec3 rot = boat.getViewVector(1.0f);
                        double x = boat.getX() + (i == 1 ? -rot.z : rot.z);
                        double z = boat.getZ() + (i == 1 ? rot.x : -rot.x);
                        Vec3 paddlePos = new Vec3(x, y, z);
                        Vec3 dir = Vec3.directionFromRotation(0, boat.getYRot()).scale(velocity);
                        Vec3 from = paddlePos;
                        Vec3 to = paddlePos.add(dir.scale(2));
                        nodesAffected.addAll(nodeTrail(from.x, from.z, to.x, to.z, y, WakesConfig.GENERAL.paddleStrength.get(), velocity));
                    }
                }
            }
            return nodesAffected;
        }

        public static Set<WakeNode> nodeTrail(double fromX, double fromZ, double toX, double toZ, int y, float waveStrength, double velocity) {
            int res = WakeHandler.resolution.res;
            int x1 = (int) (fromX * res);
            int z1 = (int) (fromZ * res);
            int x2 = (int) (toX * res);
            int z2 = (int) (toZ * res);

            ArrayList<Long> pixelsAffected = new ArrayList<>();
            WakesUtils.bresenhamLine(x1, z1, x2, z2, pixelsAffected);
            return pixelsToNodes(pixelsAffected, y, waveStrength, velocity);
        }

        public static Set<WakeNode> thickNodeTrail(double fromX, double fromZ, double toX, double toZ, int y, float waveStrength, double velocity, float width) {
            // Skip wake generation for very large distances (likely teleports)
            double distanceSq = (toX - fromX) * (toX - fromX) + (toZ - fromZ) * (toZ - fromZ);
            if (distanceSq > 400) { // 20 blocks squared
                return new HashSet<>();
            }

            int res = WakeHandler.resolution.res;
            int x1 = (int) (fromX * res);
            int z1 = (int) (fromZ * res);
            int x2 = (int) (toX * res);
            int z2 = (int) (toZ * res);
            int w = (int) (0.8 * width * res / 2);

            // Limit line length to prevent performance issues
            float len = (float) Math.sqrt(Math.pow(z1 - z2, 2) + Math.pow(x2 - x1, 2));
            if (len > 1000) { // Arbitrary limit for pixel operations
                return new HashSet<>();
            }

            float nx = (z1 - z2) / len;
            float nz = (x2 - x1) / len;
            ArrayList<Long> pixelsAffected = new ArrayList<>();
            for (int i = -w; i < w; i++) {
                WakesUtils.bresenhamLine((int) (x1 + nx * i), (int) (z1 + nz * i),
                        (int) (x2 + nx * i), (int) (z2 + nz * i), pixelsAffected);
            }
            return pixelsToNodes(pixelsAffected, y, waveStrength, velocity);
        }

        public static Set<WakeNode> nodeLine(double x, int y, double z, float waveStrength, Vec3 velocity, float width) {
            int res = WakeHandler.resolution.res;
            Vec3 dir = velocity.normalize();
            double nx = -dir.z;
            double nz = dir.x;
            int w = (int) (0.8 * width * res / 2);

            int x1 = (int) (x * res - nx * w);
            int z1 = (int) (z * res - nz * w);
            int x2 = (int) (x * res + nx * w);
            int z2 = (int) (z * res + nz * w);

            ArrayList<Long> pixelsAffected = new ArrayList<>();
            WakesUtils.bresenhamLine(x1, z1, x2, z2, pixelsAffected);
            return pixelsToNodes(pixelsAffected, y, waveStrength, velocity.horizontalDistance());
        }

        private static Set<WakeNode> pixelsToNodes(ArrayList<Long> pixelsAffected, int y, float waveStrength, double velocity) {
            int res = WakeHandler.resolution.res;
            int power = (int) (Math.log(res) / Math.log(2));
            HashMap<Long, HashSet<Long>> pixelsInNodes = new HashMap<>();
            for (Long pixel : pixelsAffected) {
                int[] pos = WakesUtils.longAsPos(pixel);
                long k = WakesUtils.posAsLong(pos[0] >> power, pos[1] >> power);
                pos[0] %= res;
                pos[1] %= res;
                long v = WakesUtils.posAsLong(pos[0], pos[1]);
                if (pixelsInNodes.containsKey(k)) {
                    pixelsInNodes.get(k).add(v);
                } else {
                    HashSet<Long> set = new HashSet<>();
                    set.add(v);
                    pixelsInNodes.put(k, set);
                }
            }
            Set<WakeNode> nodesAffected = new HashSet<>();
            for (Long nodePos : pixelsInNodes.keySet()) {
                WakeNode node = new WakeNode(nodePos, y);
                for (Long subPos : pixelsInNodes.get(nodePos)) {
                    node.simulationNode.setInitialValue(subPos, (int) (waveStrength * velocity));
                }
                nodesAffected.add(node);
            }
            return nodesAffected;
        }
    }
}
