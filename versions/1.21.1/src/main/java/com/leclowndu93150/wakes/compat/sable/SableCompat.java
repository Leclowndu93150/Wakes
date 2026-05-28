package com.leclowndu93150.wakes.compat.sable;

import com.leclowndu93150.wakes.compat.ModCompat;
import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.simulation.WakeNode;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SableCompat {
    private static final double PROBE_BELOW = 2.0;
    private static final int NO_FLUID_RESCAN_INTERVAL = 10;
    private static final double NO_FLUID_RESCAN_MOVE_SQ = 1.0;
    private static final int STATE_PRUNE_AGE = 200;
    private static final int HULL_SCAN_INTERVAL = 20;
    private static final int SHAPE_SIGNATURE_INTERVAL = 40;
    private static final double LOCAL_SCAN_INFLATE = 0.35;
    private static final double WATERLINE_TOLERANCE = 0.75;
    private static final double FOOTPRINT_BASE_TOLERANCE = 1.15;
    private static final double FOOTPRINT_SWEEP_MARGIN = 0.75;
    private static final double MIN_TRAIL_SPEED = 1.0e-3;
    private static final double MIN_SPLASH_SPEED = 0.08;
    private static final double MAX_TRAIL_DISTANCE_SQ = 400.0;
    private static final float HULL_WAKE_WIDTH = 1.35f;
    private static final int[][] FOOTPRINT_NEIGHBORS = new int[][]{
            {1, -1, 0},
            {1, 0, 0},
            {1, 1, 0},
            {0, -1, 1},
            {0, 0, 1},
            {0, 1, 1}
    };
    private static final int[][] HORIZONTAL_NEIGHBORS = new int[][]{
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
    };

    private static final Map<UUID, SubLevelWakeState> STATES = new HashMap<>();

    public static void tickSubLevelWake(SubLevel subLevel) {
        if (!ModCompat.isSableLoaded() || subLevel.isRemoved()) return;

        Level level = subLevel.getLevel();
        if (!level.isClientSide) return;

        WakeHandler wakeHandler = WakeHandler.getInstance(level).orElse(null);
        if (wakeHandler == null || WakesConfig.GENERAL.disableMod.get()) return;

        UUID id = subLevel.getUniqueId();
        if (id == null) return;

        long gameTime = level.getGameTime();
        pruneStaleStates(gameTime);

        {
            BoundingBox3dc bounds = subLevel.boundingBox();
            SubLevelWakeState state = STATES.computeIfAbsent(id, k -> new SubLevelWakeState());
            state.lastSeenTime = gameTime;
            double centerY = (bounds.minY() + bounds.maxY()) * 0.5;
            double subLevelVerticalSpeed = Double.isNaN(state.lastCenterY) ? 0.0 : Math.abs(centerY - state.lastCenterY);
            boolean shapeChanged = checkLoadedPlotShapeChanged(subLevel, state, gameTime);

            double centerX = (bounds.minX() + bounds.maxX()) * 0.5;
            double centerZ = (bounds.minZ() + bounds.maxZ()) * 0.5;
            if (!state.touchingWater && gameTime < state.noFluidRescanTime
                    && sqDist(centerX, centerZ, state.noFluidCenterX, state.noFluidCenterZ) < NO_FLUID_RESCAN_MOVE_SQ) {
                state.lastCenterY = centerY;
                return;
            }

            FluidSurface surface = findFluidSurfaceInBounds(level, bounds);
            if (surface == null) {
                state.noFluidRescanTime = gameTime + NO_FLUID_RESCAN_INTERVAL;
                state.noFluidCenterX = centerX;
                state.noFluidCenterZ = centerZ;
                if (state.touchingWater && subLevelVerticalSpeed >= MIN_SPLASH_SPEED && !state.prevWorldPositions.isEmpty() && !Double.isNaN(state.lastWaterY)) {
                    insertConnectedFootprintNodes(wakeHandler, state.prevWorldPositions, (int) Math.floor(state.lastWaterY), WakesConfig.GENERAL.splashStrength.get(), subLevelVerticalSpeed);
                }
                state.touchingWater = false;
                state.prevWorldPositions.clear();
                state.lastCenterY = centerY;
                return;
            }

            boolean justEnteredWater = !state.touchingWater;
            boolean waterLevelChanged = Double.isNaN(state.lastWaterY) || Math.abs(state.lastWaterY - surface.height) > 0.25;
            boolean verticalMotion = subLevelVerticalSpeed >= MIN_SPLASH_SPEED;
            boolean boundsTouchWaterPlane = bounds.minY() <= surface.height + WATERLINE_TOLERANCE && bounds.maxY() >= surface.height - WATERLINE_TOLERANCE;
            double footprintTolerance = footprintTolerance(subLevelVerticalSpeed);
            if (state.edgeBlocks.isEmpty() || state.contactBlocks.isEmpty() || state.footprintBlocks.isEmpty() || gameTime >= state.nextHullScanTime || waterLevelChanged || verticalMotion || shapeChanged) {
                HullScan scan = scanHullAtWaterline(level, subLevel, surface.height);
                state.edgeBlocks = scan.edgeBlocks;
                state.contactBlocks = scan.contactBlocks;
                state.footprintBlocks = scanHullFootprintBlocks(level, subLevel, surface.height, footprintTolerance);
                state.nextHullScanTime = gameTime + HULL_SCAN_INTERVAL;
            }

            if (state.edgeBlocks.isEmpty()) {
                Map<BlockPos, Vec3> fallbackFootprint = boundsTouchWaterPlane ? scanHullFootprint(level, subLevel, surface.height, footprintTolerance) : Map.of();
                if (!fallbackFootprint.isEmpty()) {
                    double fallbackVerticalSpeed = estimateVerticalSpeed(level, subLevel, fallbackFootprint.keySet(), subLevelVerticalSpeed);
                    if ((!state.touchingWater || fallbackVerticalSpeed >= MIN_SPLASH_SPEED) && fallbackVerticalSpeed >= MIN_SPLASH_SPEED) {
                        insertConnectedFootprintNodes(wakeHandler, fallbackFootprint, (int) Math.floor(surface.height), WakesConfig.GENERAL.splashStrength.get(), fallbackVerticalSpeed);
                    }
                    state.prevWorldPositions = fallbackFootprint;
                    state.touchingWater = true;
                    state.lastWaterY = surface.height;
                    state.lastCenterY = centerY;
                    return;
                }

                if (state.touchingWater && subLevelVerticalSpeed >= MIN_SPLASH_SPEED && !state.prevWorldPositions.isEmpty()) {
                    double waterY = Double.isNaN(state.lastWaterY) ? surface.height : state.lastWaterY;
                    insertConnectedFootprintNodes(wakeHandler, state.prevWorldPositions, (int) Math.floor(waterY), WakesConfig.GENERAL.splashStrength.get(), subLevelVerticalSpeed);
                }
                state.touchingWater = false;
                state.lastWaterY = surface.height;
                state.lastCenterY = centerY;
                state.prevWorldPositions.clear();
                return;
            }

            int y = (int) Math.floor(surface.height);
            Map<BlockPos, Vec3> currentWorldPositions = new HashMap<>(state.edgeBlocks.size());
            Map<BlockPos, Vec3> footprintPositions = projectContactBlocks(level, state.contactBlocks, surface.height);
            Map<BlockPos, Vec3> filledFootprintPositions = projectFootprintBlocks(level, state.footprintBlocks, surface.height, footprintTolerance);
            if (!filledFootprintPositions.isEmpty()) {
                footprintPositions = filledFootprintPositions;
            }
            double contactHorizontalSpeed = 0.0;
            double contactVerticalSpeed = subLevelVerticalSpeed;
            for (BlockPos plotBlock : state.edgeBlocks) {
                Vec3 localCenter = Vec3.atCenterOf(plotBlock);
                Vec3 projected = Sable.HELPER.projectOutOfSubLevel(level, localCenter);
                if (Math.abs(projected.y - surface.height) > WATERLINE_TOLERANCE) continue;

                Vec3 current = new Vec3(projected.x, surface.height, projected.z);
                currentWorldPositions.put(plotBlock, current);

                Vec3 previous = state.prevWorldPositions.get(plotBlock);
                if (previous == null) continue;

                Vec3 displacement = current.subtract(previous);
                double speed = displacement.horizontalDistance();
                if (speed < MIN_TRAIL_SPEED || previous.distanceToSqr(current) > MAX_TRAIL_DISTANCE_SQ) continue;

                contactHorizontalSpeed = Math.max(contactHorizontalSpeed, speed);
            }

            if (currentWorldPositions.isEmpty() && boundsTouchWaterPlane) {
                currentWorldPositions = scanHullFootprint(level, subLevel, surface.height, footprintTolerance);
                if (!currentWorldPositions.isEmpty()) {
                    contactVerticalSpeed = estimateVerticalSpeed(level, subLevel, currentWorldPositions.keySet(), contactVerticalSpeed);
                }
            }
            if (footprintPositions.isEmpty()) {
                footprintPositions = currentWorldPositions;
            }

            Map<BlockPos, Vec3> nextWorldPositions = new HashMap<>(footprintPositions);
            nextWorldPositions.putAll(currentWorldPositions);
            double sweptHorizontalSpeed = insertSweptFootprintTrails(
                    wakeHandler,
                    state.prevWorldPositions,
                    footprintPositions,
                    y,
                    WakesConfig.GENERAL.initialStrength.get());
            contactHorizontalSpeed = Math.max(contactHorizontalSpeed, sweptHorizontalSpeed);
            if (contactHorizontalSpeed >= MIN_TRAIL_SPEED) {
                insertConnectedFootprintNodes(wakeHandler, footprintPositions, y, WakesConfig.GENERAL.initialStrength.get(), contactHorizontalSpeed);
            }
            if ((justEnteredWater || contactVerticalSpeed > MIN_SPLASH_SPEED) && contactVerticalSpeed >= MIN_SPLASH_SPEED) {
                insertConnectedFootprintNodes(wakeHandler, footprintPositions, y, WakesConfig.GENERAL.splashStrength.get(), contactVerticalSpeed);
            }

            state.prevWorldPositions = nextWorldPositions;
            state.touchingWater = true;
            state.lastWaterY = surface.height;
            state.lastCenterY = centerY;
        }
    }

    private static void pruneStaleStates(long gameTime) {
        STATES.values().removeIf(state -> gameTime - state.lastSeenTime > STATE_PRUNE_AGE);
    }

    public static void invalidateShape(LevelPlot plot) {
        if (!ModCompat.isSableLoaded()) return;
        invalidateShape(plot.getSubLevel());
    }

    public static void invalidateShapeFromPlot(Object plotObj) {
        if (!ModCompat.isSableLoaded() || !(plotObj instanceof LevelPlot plot)) return;
        invalidateShape(plot);
    }

    public static void invalidateShape(SubLevel subLevel) {
        UUID id = subLevel.getUniqueId();
        if (id == null) return;

        SubLevelWakeState state = STATES.get(id);
        if (state == null) return;

        clearShapeCache(state);
    }

    private static boolean checkLoadedPlotShapeChanged(SubLevel subLevel, SubLevelWakeState state, long gameTime) {
        if (gameTime < state.nextShapeSignatureCheckTime) return false;

        state.nextShapeSignatureCheckTime = gameTime + SHAPE_SIGNATURE_INTERVAL;
        long shapeSignature = computeLoadedPlotSignature(subLevel);
        if (state.shapeSignature == shapeSignature) return false;

        clearShapeCache(state);
        state.shapeSignature = shapeSignature;
        return true;
    }

    private static void clearShapeCache(SubLevelWakeState state) {
        state.edgeBlocks = List.of();
        state.contactBlocks = List.of();
        state.footprintBlocks = List.of();
        state.prevWorldPositions.clear();
        state.nextHullScanTime = 0L;
        state.nextShapeSignatureCheckTime = 0L;
        state.shapeSignature = Long.MIN_VALUE;
    }

    private static HullScan scanHullAtWaterline(Level level, SubLevel subLevel, float worldWaterY) {
        BoundingBox3i localBounds = computeLoadedPlotBounds(subLevel);
        if (localBounds == null) return HullScan.EMPTY;

        Pose3dc scanPose = getScanPose(subLevel);
        AABB worldSlice = waterSurfaceSlab(subLevel.boundingBox(), worldWaterY, WATERLINE_TOLERANCE);
        AABB localSlice = worldAabbToLocalAabb(worldSlice, scanPose).inflate(LOCAL_SCAN_INFLATE);

        ScanBounds scanBounds = clampScanBounds(localSlice, localBounds);
        if (scanBounds.isEmpty()) return HullScan.EMPTY;

        List<BlockPos> edgeBlocks = new ArrayList<>();
        List<BlockPos> contactBlocks = new ArrayList<>();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int y = scanBounds.minY; y <= scanBounds.maxY; y++) {
            for (int x = scanBounds.minX; x <= scanBounds.maxX; x++) {
                for (int z = scanBounds.minZ; z <= scanBounds.maxZ; z++) {
                    mutable.set(x, y, z);
                    if (!isSolidHullBlock(level, mutable)) continue;

                    Vec3 worldPos = Sable.HELPER.projectOutOfSubLevel(level, Vec3.atCenterOf(mutable));
                    if (Math.abs(worldPos.y - worldWaterY) > WATERLINE_TOLERANCE) continue;
                    contactBlocks.add(new BlockPos(x, y, z));
                    if (isHorizontalEdge(level, mutable, x, y, z)) {
                        edgeBlocks.add(new BlockPos(x, y, z));
                    }
                }
            }
        }

        return new HullScan(edgeBlocks, contactBlocks);
    }

    private static Map<BlockPos, Vec3> projectContactBlocks(Level level, List<BlockPos> contactBlocks, float worldWaterY) {
        Map<BlockPos, Vec3> positions = new HashMap<>(contactBlocks.size());
        for (BlockPos plotBlock : contactBlocks) {
            Vec3 projected = Sable.HELPER.projectOutOfSubLevel(level, Vec3.atCenterOf(plotBlock));
            if (Math.abs(projected.y - worldWaterY) <= WATERLINE_TOLERANCE) {
                positions.put(plotBlock, new Vec3(projected.x, worldWaterY, projected.z));
            }
        }
        return positions;
    }

    private static Map<BlockPos, Vec3> scanHullFootprint(Level level, SubLevel subLevel, float worldWaterY, double maxDistanceFromWater) {
        List<BlockPos> footprintBlocks = scanHullFootprintBlocks(level, subLevel, worldWaterY, maxDistanceFromWater);
        return projectFootprintBlocks(level, footprintBlocks, worldWaterY, maxDistanceFromWater);
    }

    private static List<BlockPos> scanHullFootprintBlocks(Level level, SubLevel subLevel, float worldWaterY, double maxDistanceFromWater) {
        BoundingBox3i localBounds = computeLoadedPlotBounds(subLevel);
        if (localBounds == null) return List.of();

        Pose3dc scanPose = getScanPose(subLevel);
        AABB worldSlice = waterSurfaceSlab(subLevel.boundingBox(), worldWaterY, maxDistanceFromWater);
        AABB localSlice = worldAabbToLocalAabb(worldSlice, scanPose).inflate(LOCAL_SCAN_INFLATE);

        ScanBounds scanBounds = clampScanBounds(localSlice, localBounds);
        if (scanBounds.minX > scanBounds.maxX || scanBounds.minZ > scanBounds.maxZ) return List.of();
        if (scanBounds.isEmpty()) return List.of();

        Map<Long, FootprintCandidate> candidates = new HashMap<>();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int y = scanBounds.minY; y <= scanBounds.maxY; y++) {
            for (int x = scanBounds.minX; x <= scanBounds.maxX; x++) {
                for (int z = scanBounds.minZ; z <= scanBounds.maxZ; z++) {
                    mutable.set(x, y, z);
                    if (!isSolidHullBlock(level, mutable)) continue;

                    Vec3 worldPos = Sable.HELPER.projectOutOfSubLevel(level, Vec3.atCenterOf(mutable));
                    double distance = Math.abs(worldPos.y - worldWaterY);
                    if (distance > maxDistanceFromWater) continue;

                    long key = columnKey(x, z);
                    FootprintCandidate candidate = candidates.get(key);
                    if (candidate == null || distance < candidate.distance) {
                        candidates.put(key, new FootprintCandidate(new BlockPos(x, y, z), distance));
                    }
                }
            }
        }

        List<BlockPos> footprint = new ArrayList<>(candidates.size());
        for (FootprintCandidate candidate : candidates.values()) {
            footprint.add(candidate.pos);
        }
        return footprint;
    }

    private static Map<BlockPos, Vec3> projectFootprintBlocks(Level level, List<BlockPos> footprintBlocks, float worldWaterY, double maxDistanceFromWater) {
        Map<BlockPos, Vec3> footprint = new HashMap<>(footprintBlocks.size());
        for (BlockPos block : footprintBlocks) {
            Vec3 worldPos = Sable.HELPER.projectOutOfSubLevel(level, Vec3.atCenterOf(block));
            if (Math.abs(worldPos.y - worldWaterY) > maxDistanceFromWater) continue;
            footprint.put(block, new Vec3(worldPos.x, worldWaterY, worldPos.z));
        }
        return footprint;
    }

    private static double footprintTolerance(double subLevelVerticalSpeed) {
        return Math.max(FOOTPRINT_BASE_TOLERANCE, subLevelVerticalSpeed + FOOTPRINT_SWEEP_MARGIN);
    }

    private static long columnKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static double estimateVerticalSpeed(Level level, SubLevel subLevel, Iterable<BlockPos> positions, double fallback) {
        double speed = fallback;
        int samples = 0;
        for (BlockPos pos : positions) {
            Vec3 velocityPerTick = Sable.HELPER.getVelocity(level, subLevel, Vec3.atCenterOf(pos)).scale(1.0 / 20.0);
            speed = Math.max(speed, Math.abs(velocityPerTick.y));
            if (++samples >= 16) break;
        }
        return speed;
    }

    private static BoundingBox3i computeLoadedPlotBounds(SubLevel subLevel) {
        BoundingBox3ic plotBounds = subLevel.getPlot().getBoundingBox();
        if (!isEmpty(plotBounds)) {
            return new BoundingBox3i(plotBounds);
        }

        BoundingBox3i bounds = null;
        BoundingBox3i chunkBounds = new BoundingBox3i();
        for (PlotChunkHolder chunk : subLevel.getPlot().getLoadedChunks()) {
            BoundingBox3ic chunkLocalBounds = chunk.getBoundingBox();
            if (chunkLocalBounds == null) continue;

            ChunkPos chunkPos = chunk.getPos();
            chunkLocalBounds.move(chunkPos.getMinBlockX(), 0, chunkPos.getMinBlockZ(), chunkBounds);
            if (bounds == null) {
                bounds = new BoundingBox3i(chunkBounds);
            } else {
                bounds.expandTo(chunkBounds, bounds);
            }
        }
        return bounds;
    }

    private static long computeLoadedPlotSignature(SubLevel subLevel) {
        long signature = 1125899906842597L;
        BoundingBox3ic plotBounds = subLevel.getPlot().getBoundingBox();
        if (!isEmpty(plotBounds)) {
            signature = signature * 31L + plotBounds.minX();
            signature = signature * 31L + plotBounds.minY();
            signature = signature * 31L + plotBounds.minZ();
            signature = signature * 31L + plotBounds.maxX();
            signature = signature * 31L + plotBounds.maxY();
            signature = signature * 31L + plotBounds.maxZ();
        }

        for (PlotChunkHolder chunk : subLevel.getPlot().getLoadedChunks()) {
            ChunkPos chunkPos = chunk.getPos();
            signature = signature * 31L + chunkPos.x;
            signature = signature * 31L + chunkPos.z;

            BoundingBox3ic bounds = chunk.getBoundingBox();
            if (bounds == null) {
                signature = signature * 31L - 1L;
                continue;
            }

            signature = signature * 31L + bounds.minX();
            signature = signature * 31L + bounds.minY();
            signature = signature * 31L + bounds.minZ();
            signature = signature * 31L + bounds.maxX();
            signature = signature * 31L + bounds.maxY();
            signature = signature * 31L + bounds.maxZ();
        }
        return signature;
    }

    private static boolean isEmpty(BoundingBox3ic bounds) {
        return bounds == null || bounds.minX() > bounds.maxX() || bounds.minY() > bounds.maxY() || bounds.minZ() > bounds.maxZ();
    }

    private static Pose3dc getScanPose(SubLevel subLevel) {
        if (subLevel instanceof ClientSubLevelAccess client) {
            return client.renderPose();
        }
        return subLevel.logicalPose();
    }

    private static AABB worldAabbToLocalAabb(AABB worldBox, Pose3dc pose) {
        Vec3[] corners = new Vec3[]{
                new Vec3(worldBox.minX, worldBox.minY, worldBox.minZ),
                new Vec3(worldBox.minX, worldBox.minY, worldBox.maxZ),
                new Vec3(worldBox.minX, worldBox.maxY, worldBox.minZ),
                new Vec3(worldBox.minX, worldBox.maxY, worldBox.maxZ),
                new Vec3(worldBox.maxX, worldBox.minY, worldBox.minZ),
                new Vec3(worldBox.maxX, worldBox.minY, worldBox.maxZ),
                new Vec3(worldBox.maxX, worldBox.maxY, worldBox.minZ),
                new Vec3(worldBox.maxX, worldBox.maxY, worldBox.maxZ)
        };

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (Vec3 corner : corners) {
            Vec3 local = pose.transformPositionInverse(corner);
            minX = Math.min(minX, local.x);
            minY = Math.min(minY, local.y);
            minZ = Math.min(minZ, local.z);
            maxX = Math.max(maxX, local.x);
            maxY = Math.max(maxY, local.y);
            maxZ = Math.max(maxZ, local.z);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static AABB waterSurfaceSlab(BoundingBox3dc bounds, float worldWaterY, double halfHeight) {
        return bounds.toMojang().setMinY(worldWaterY - halfHeight).setMaxY(worldWaterY + halfHeight);
    }

    private static ScanBounds clampScanBounds(AABB localSlice, BoundingBox3i localBounds) {
        int minX = Math.max((int) Math.floor(localSlice.minX), localBounds.minX());
        int maxX = Math.min((int) Math.ceil(localSlice.maxX), localBounds.maxX());
        int minY = Math.max((int) Math.floor(localSlice.minY), localBounds.minY());
        int maxY = Math.min((int) Math.ceil(localSlice.maxY), localBounds.maxY());
        int minZ = Math.max((int) Math.floor(localSlice.minZ), localBounds.minZ());
        int maxZ = Math.min((int) Math.ceil(localSlice.maxZ), localBounds.maxZ());

        return new ScanBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static boolean isSolidHullBlock(Level level, BlockPos pos) {
        FluidState fluidState = level.getFluidState(pos);
        if (!fluidState.isEmpty()) return false;

        BlockState blockState = level.getBlockState(pos);
        return !blockState.isAir() && !blockState.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean isHorizontalEdge(Level level, BlockPos.MutableBlockPos mutable, int x, int y, int z) {
        for (int[] neighbor : HORIZONTAL_NEIGHBORS) {
            mutable.set(x + neighbor[0], y, z + neighbor[1]);
            if (!isSolidHullBlock(level, mutable)) {
                mutable.set(x, y, z);
                return true;
            }
        }
        mutable.set(x, y, z);
        return false;
    }

    private static void insertConnectedFootprintNodes(WakeHandler wakeHandler, Map<BlockPos, Vec3> positions, int y, float strength, double velocity) {
        List<WakeNode.Factory.Trail> trails = new ArrayList<>(positions.size() * 2);
        for (Map.Entry<BlockPos, Vec3> entry : positions.entrySet()) {
            BlockPos plotPos = entry.getKey();
            Vec3 pos = entry.getValue();
            trails.add(new WakeNode.Factory.Trail(pos.x, pos.z, pos.x, pos.z));

            for (int[] offset : FOOTPRINT_NEIGHBORS) {
                Vec3 neighbor = positions.get(plotPos.offset(offset[0], offset[1], offset[2]));
                if (neighbor == null) continue;

                trails.add(new WakeNode.Factory.Trail(pos.x, pos.z, neighbor.x, neighbor.z));
            }
        }

        for (WakeNode node : WakeNode.Factory.nodeTrails(trails, y, strength, velocity)) {
            wakeHandler.insert(node);
        }
    }

    private static double insertSweptFootprintTrails(WakeHandler wakeHandler, Map<BlockPos, Vec3> previousPositions, Map<BlockPos, Vec3> currentPositions, int y, float strength) {
        List<WakeNode.Factory.Trail> trails = new ArrayList<>(currentPositions.size());
        double maxSpeed = 0.0;

        for (Map.Entry<BlockPos, Vec3> entry : currentPositions.entrySet()) {
            BlockPos plotBlock = entry.getKey();
            Vec3 previous = previousPositions.get(plotBlock);
            if (previous == null) continue;

            Vec3 current = entry.getValue();
            double speed = current.subtract(previous).horizontalDistance();
            if (speed < MIN_TRAIL_SPEED || previous.distanceToSqr(current) > MAX_TRAIL_DISTANCE_SQ) continue;

            maxSpeed = Math.max(maxSpeed, speed);
            trails.add(new WakeNode.Factory.Trail(previous.x, previous.z, current.x, current.z));
        }

        if (trails.isEmpty()) return 0.0;

        for (WakeNode node : WakeNode.Factory.thickNodeTrails(trails, y, strength, maxSpeed, HULL_WAKE_WIDTH)) {
            wakeHandler.insert(node);
        }
        return maxSpeed;
    }

    private static double sqDist(double x1, double z1, double x2, double z2) {
        if (Double.isNaN(x2) || Double.isNaN(z2)) return Double.POSITIVE_INFINITY;
        double dx = x1 - x2;
        double dz = z1 - z2;
        return dx * dx + dz * dz;
    }

    private static FluidSurface findFluidSurfaceInBounds(Level level, BoundingBox3dc bounds) {
        double centerX = (bounds.minX() + bounds.maxX()) * 0.5;
        double centerZ = (bounds.minZ() + bounds.maxZ()) * 0.5;

        FluidSurface surface = findWorldFluidSurfaceAt(level, centerX, centerZ, bounds.minY() - PROBE_BELOW, bounds.maxY() + 1.0);
        if (surface != null) return surface;

        for (double px : new double[]{bounds.minX(), bounds.maxX()}) {
            for (double pz : new double[]{bounds.minZ(), bounds.maxZ()}) {
                surface = findWorldFluidSurfaceAt(level, px, pz, bounds.minY() - PROBE_BELOW, bounds.maxY() + 1.0);
                if (surface != null) return surface;
            }
        }

        return findFluidSurfaceOnBoundsPerimeter(level, bounds);
    }

    @Nullable
    private static FluidSurface findFluidSurfaceOnBoundsPerimeter(Level level, BoundingBox3dc bounds) {
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        int minX = (int) Math.floor(bounds.minX());
        int maxX = (int) Math.ceil(bounds.maxX());
        int minY = (int) Math.floor(bounds.minY() - PROBE_BELOW);
        int maxY = (int) Math.ceil(bounds.maxY() + 1.0);
        int minZ = (int) Math.floor(bounds.minZ());
        int maxZ = (int) Math.ceil(bounds.maxZ());

        for (int y = maxY; y >= minY; y--) {
            for (int x = minX; x <= maxX; x++) {
                blockPos.set(x, y, minZ);
                FluidSurface surface = getFluidSurface(level, blockPos);
                if (surface != null) return surface;

                if (maxZ != minZ) {
                    blockPos.set(x, y, maxZ);
                    surface = getFluidSurface(level, blockPos);
                    if (surface != null) return surface;
                }
            }

            for (int z = minZ + 1; z < maxZ; z++) {
                blockPos.set(minX, y, z);
                FluidSurface surface = getFluidSurface(level, blockPos);
                if (surface != null) return surface;

                if (maxX != minX) {
                    blockPos.set(maxX, y, z);
                    surface = getFluidSurface(level, blockPos);
                    if (surface != null) return surface;
                }
            }
        }
        return null;
    }

    @Nullable
    public static Object findSubLevelWithFluidUnder(Entity entity) {
        if (!ModCompat.isSableLoaded()) return null;

        Level level = entity.level();
        SubLevelAccess tracking = Sable.HELPER.getTrackingOrVehicleSubLevel(entity);
        if (tracking != null && hasFluidSurfaceForEntity(tracking, level, entity)) {
            return tracking;
        }
        return null;
    }

    private static boolean hasFluidSurfaceForEntity(SubLevelAccess subLevel, Level level, Entity entity) {
        Pose3dc pose = subLevel.logicalPose();
        Vec3 localPos = pose.transformPositionInverse(entity.position());
        AABB box = entity.getBoundingBox();
        Vec3 localMin = pose.transformPositionInverse(new Vec3(box.minX, box.minY, box.minZ));
        Vec3 localMax = pose.transformPositionInverse(new Vec3(box.maxX, box.maxY, box.maxZ));

        double minY = Math.min(localMin.y, localMax.y);
        double maxY = Math.max(localMin.y, localMax.y);

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (int y = (int) Math.floor(minY); y <= (int) Math.floor(maxY); y++) {
            blockPos.set((int) Math.floor(localPos.x), y, (int) Math.floor(localPos.z));
            FluidState fluidState = level.getFluidState(blockPos);
            if (!fluidState.isEmpty() && WakesConfig.getFluidWhitelist().contains(fluidState.getType())) {
                double fluidHeight = blockPos.getY() + fluidState.getHeight(level, blockPos);
                if (maxY > fluidHeight) return true;
            }
        }
        return false;
    }

    public static float getLocalFluidLevel(Object subLevelObj, Level level, Entity entity) {
        if (!ModCompat.isSableLoaded()) return (float) entity.getY();

        SubLevelAccess subLevel = (SubLevelAccess) subLevelObj;
        Pose3dc pose = subLevel.logicalPose();
        Vec3 localPos = pose.transformPositionInverse(entity.position());
        AABB box = entity.getBoundingBox();
        Vec3 localMin = pose.transformPositionInverse(new Vec3(box.minX, box.minY, box.minZ));
        Vec3 localMax = pose.transformPositionInverse(new Vec3(box.maxX, box.maxY, box.maxZ));

        double minY = Math.min(localMin.y, localMax.y);
        double maxY = Math.max(localMin.y, localMax.y);

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (int y = (int) Math.floor(minY); y < (int) Math.ceil(maxY); y++) {
            blockPos.set((int) Math.floor(localPos.x), y, (int) Math.floor(localPos.z));
            FluidState fluidState = level.getFluidState(blockPos);
            if (WakesConfig.getFluidWhitelist().contains(fluidState.getType()) && fluidState.isSource()) {
                float f = fluidState.getHeight(level, blockPos);
                if (f > 0 && f < 1.0f) return blockPos.getY() + f;
            }
        }
        return (float) (localPos.y + 1);
    }

    public static Vec3 toLocalPos(Object subLevelObj, Vec3 globalPos) {
        if (!ModCompat.isSableLoaded()) return globalPos;

        SubLevelAccess subLevel = (SubLevelAccess) subLevelObj;
        return subLevel.logicalPose().transformPositionInverse(globalPos);
    }

    @Nullable
    public static Object findSubLevelAtBlock(Level level, double blockX, double blockZ) {
        if (!ModCompat.isSableLoaded()) return null;

        return Sable.HELPER.getContaining(level, blockX, blockZ);
    }

    public static double[] transformPlotToGlobal(Object subLevelObj, double localX, double localY, double localZ, float partialTick) {
        if (!ModCompat.isSableLoaded()) return new double[]{localX, localY, localZ};

        SubLevelAccess subLevel = (SubLevelAccess) subLevelObj;
        Pose3dc pose;
        if (subLevel instanceof ClientSubLevelAccess client) {
            pose = client.renderPose(partialTick);
        } else {
            pose = subLevel.logicalPose();
        }
        Vec3 result = pose.transformPosition(new Vec3(localX, localY, localZ));
        return new double[]{result.x, result.y, result.z};
    }

    @Nullable
    private static FluidSurface findWorldFluidSurfaceAt(Level level, double x, double z, double minY, double maxY) {
        if (Sable.HELPER.getContaining(level, x, z) != null) return null;

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (int y = (int) Math.ceil(maxY); y >= (int) Math.floor(minY); y--) {
            blockPos.set((int) Math.floor(x), y, (int) Math.floor(z));
            FluidSurface surface = getFluidSurface(level, blockPos);
            if (surface != null) return surface;
        }
        return null;
    }

    @Nullable
    private static FluidSurface getFluidSurface(Level level, BlockPos pos) {
        FluidState fluidState = level.getFluidState(pos);
        if (fluidState.isEmpty() || !fluidState.isSource() || !WakesConfig.getFluidWhitelist().contains(fluidState.getType())) {
            return null;
        }
        if (!level.getFluidState(pos.above()).isEmpty()) {
            return null;
        }
        float height = (float) (pos.getY() + fluidState.getHeight(level, pos));
        return new FluidSurface(height);
    }

    private static class SubLevelWakeState {
        List<BlockPos> edgeBlocks = List.of();
        List<BlockPos> contactBlocks = List.of();
        List<BlockPos> footprintBlocks = List.of();
        Map<BlockPos, Vec3> prevWorldPositions = new HashMap<>();
        long nextHullScanTime = 0L;
        long nextShapeSignatureCheckTime = 0L;
        long shapeSignature = Long.MIN_VALUE;
        double lastWaterY = Double.NaN;
        double lastCenterY = Double.NaN;
        boolean touchingWater = false;
        long noFluidRescanTime = Long.MIN_VALUE;
        double noFluidCenterX = Double.NaN;
        double noFluidCenterZ = Double.NaN;
        long lastSeenTime = Long.MIN_VALUE;
    }

    private static class HullScan {
        static final HullScan EMPTY = new HullScan(List.of(), List.of());

        final List<BlockPos> edgeBlocks;
        final List<BlockPos> contactBlocks;

        HullScan(List<BlockPos> edgeBlocks, List<BlockPos> contactBlocks) {
            this.edgeBlocks = edgeBlocks;
            this.contactBlocks = contactBlocks;
        }
    }

    private static class FootprintCandidate {
        final BlockPos pos;
        final double distance;

        FootprintCandidate(BlockPos pos, double distance) {
            this.pos = pos;
            this.distance = distance;
        }
    }

    private static class ScanBounds {
        final int minX;
        final int minY;
        final int minZ;
        final int maxX;
        final int maxY;
        final int maxZ;

        ScanBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        boolean isEmpty() {
            return minX > maxX || minY > maxY || minZ > maxZ;
        }
    }

    private static class FluidSurface {
        final float height;

        FluidSurface(float height) {
            this.height = height;
        }
    }
}
