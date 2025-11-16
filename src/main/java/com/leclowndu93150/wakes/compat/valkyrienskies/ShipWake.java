package com.leclowndu93150.wakes.compat.valkyrienskies;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.duck.ProducesWake;
import com.leclowndu93150.wakes.particle.ModParticles;
import com.leclowndu93150.wakes.particle.WithOwnerParticleType;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.simulation.WakeNode;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3i;
import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.world.LevelYRange;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.*;

public class ShipWake {
    private static final int MAX_WIDTH = 24;

    public static void placeWakeTrail(Ship ship) {
        if (Minecraft.getInstance().level == null) return;
        WakeHandler wakeHandler = WakeHandler.getInstance(Minecraft.getInstance().level).orElse(null);
        if (wakeHandler != null) {
            ProducesWake producer = (ProducesWake)ship;
            double velocity = producer.wakes$getHorizontalVelocity();
            float height = producer.wakes$wakeHeight();
            Iterator<WakeNode> var7;
            WakeNode node;

            Vec3 prevPos = producer.wakes$getPrevPos();

            if (prevPos != null) {
                float width = ((DynamicWakeSize)ship).getWidth();

                if (width > MAX_WIDTH) return;

                double toX = ((DynamicWakeSize)ship).getPos().x;
                double toZ = ((DynamicWakeSize)ship).getPos().z;
                
                double dx = toX - prevPos.x;
                double dz = toZ - prevPos.z;
                double distance = Math.sqrt(dx * dx + dz * dz);
                
                int maxSegmentLength = 10;
                
                if (distance > maxSegmentLength) {
                    int segments = (int) Math.ceil(distance / maxSegmentLength);
                    for (int i = 0; i < segments; i++) {
                        double t1 = (double) i / segments;
                        double t2 = (double) (i + 1) / segments;
                        
                        double x1 = prevPos.x + dx * t1;
                        double z1 = prevPos.z + dz * t1;
                        double x2 = prevPos.x + dx * t2;
                        double z2 = prevPos.z + dz * t2;
                        
                        var7 = WakeNode.Factory.thickNodeTrail(x1, z1, x2, z2, (int)Math.floor(height), com.leclowndu93150.wakes.config.WakesConfig.GENERAL.initialStrength.get(), velocity, width).iterator();
                        
                        while(var7.hasNext()) {
                            node = var7.next();
                            wakeHandler.insert(node);
                        }
                    }
                } else {
                    var7 = WakeNode.Factory.thickNodeTrail(prevPos.x, prevPos.z, toX, toZ, (int)Math.floor(height), com.leclowndu93150.wakes.config.WakesConfig.GENERAL.initialStrength.get(), velocity, width).iterator();

                    while(var7.hasNext()) {
                        node = var7.next();
                        wakeHandler.insert(node);
                    }
                }
            }
        }
    }

    public static void checkShipSize(Ship s) {
        Level level = Minecraft.getInstance().level;

        Vector3dc horizontalVelocity = new Vector3d(s.getVelocity().x(), 0, s.getVelocity().z());

        if (horizontalVelocity.length() < 0.05) return;

        double velAngle = horizontalVelocity.angleSigned(new Vector3d(0, 0, -1), new Vector3d(0, 1, 0));
        double shipAngle = VSUtils.getYaw(s.getTransform().getShipToWorldRotation());

        Direction direction = VSUtils.approximateDirection(Math.toDegrees(velAngle - shipAngle));

        Vec3 shipPos = VSUtils.getCentre(s.getWorldAABB());

        Double yLevelShip = VectorConversionsMCKt.toJOML(new Vec3(shipPos.x, ValkyrienSkiesCompat.getSeaLevel(), shipPos.z)).mulPosition(s.getWorldToShip()).y;

        int blockYLevelShip = yLevelShip.intValue();

        Vector3i minWorldPos = new Vector3i();
        Vector3i maxWorldPos = new Vector3i();

        int minY = (blockYLevelShip / 16) * 16;
        int maxY = ((blockYLevelShip / 16) * 16) + 15;

        s.getActiveChunksSet().getMinMaxWorldPos(minWorldPos, maxWorldPos, new LevelYRange(minY, maxY));

        AABBic shipBounds = s.getShipAABB();
        assert shipBounds != null;
        int shipX = shipBounds.maxX() - shipBounds.minX();
        int shipZ = shipBounds.maxZ() - shipBounds.minZ();

        if ((direction == Direction.NORTH || direction == Direction.SOUTH) && (shipX > shipZ)) {
            ((DynamicWakeSize) s).setWidth(0);
            return;
        } else if ((direction == Direction.EAST || direction == Direction.WEST) && (shipZ > shipX)) {
            ((DynamicWakeSize) s).setWidth(0);
            return;
        }

        calculateShipWidthAndOffset(level, minWorldPos, maxWorldPos, blockYLevelShip, direction, s);
    }

    private static void calculateShipWidthAndOffset(Level level, Vector3i minWorldPos, Vector3i maxWorldPos,
                                                    int blockYLevelShip, Direction direction, Ship s) {
        boolean isZAxis = (direction == Direction.NORTH || direction == Direction.SOUTH);

        int primaryMin = isZAxis ? minWorldPos.z() : minWorldPos.x();
        int primaryMax = isZAxis ? maxWorldPos.z() : maxWorldPos.x();
        int secondaryMin = isZAxis ? minWorldPos.x() : minWorldPos.z();
        int secondaryMax = isZAxis ? maxWorldPos.x() : maxWorldPos.z();
        boolean isNegativeDirection = (direction == Direction.NORTH || direction == Direction.WEST);

        List<LinkedList<BlockPos>> rows = new ArrayList<>();

        for (int primary = isNegativeDirection ? primaryMax : primaryMin;
             isNegativeDirection ? primary >= primaryMin : primary <= primaryMax;
             primary += isNegativeDirection ? -1 : 1) {

            LinkedList<BlockPos> blockPositions = new LinkedList<>();

            for (int secondary = secondaryMin; secondary <= secondaryMax; secondary++) {
                int x = isZAxis ? secondary : primary;
                int z = isZAxis ? primary : secondary;

                if (!level.getBlockState(new BlockPos(x, blockYLevelShip, z)).isAir()) {
                    blockPositions.add(new BlockPos(x, blockYLevelShip, z));
                }
            }
            if (!blockPositions.isEmpty()) {
                rows.add(blockPositions);
            }
            if (rows.size() >= 5) break;
        }

        float width = 0;
        Vector3d offset = new Vector3d();

        for(LinkedList<BlockPos> row : rows) {
            float rowWidth = getWidth(row);
            if (rowWidth > width && rowWidth <= MAX_WIDTH) {
                width = rowWidth;
                offset = getOffset(row);
                offset = new Vector3d(offset.x, 0, offset.z);
            }
        }

        ((DynamicWakeSize) s).setWidth(width);

        Vector3d offsetReal;

        if (!offset.equals(0,0,0)) {
            Vector3d shipCentre = VSUtils.getCentre(Objects.requireNonNull(s.getShipAABB()));
            offsetReal = new Vector3d(shipCentre.x, 0, shipCentre.z).sub(offset);
            ((DynamicWakeSize) s).setOffset(offsetReal.negate());
        }
    }

    private static int getWidth(LinkedList<BlockPos> blockPositions) {
        if(blockPositions.isEmpty()) return 0;
        return blockPositions.getFirst().distManhattan(blockPositions.getLast()) + 1;
    }

    private static Vector3d getOffset(LinkedList<BlockPos> blockPositions) {
        if (blockPositions.isEmpty()) return new Vector3d();

        Vec3 first = Vec3.atCenterOf(blockPositions.getFirst());
        Vec3 last = Vec3.atCenterOf(blockPositions.getLast());

        return VSUtils.averageVec(first, last);
    }
}
