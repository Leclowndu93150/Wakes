package com.leclowndu93150.wakes.utils;

import com.leclowndu93150.wakes.WakesMod;
import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.config.enums.EffectSpawningRule;
import com.leclowndu93150.wakes.duck.ProducesWake;
import com.leclowndu93150.wakes.particle.custom.SplashCloudParticle;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.simulation.WakeNode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidBase;

public class WakesUtils {

    public static void placeFallSplash(Entity entity) {
        WakeHandler wakeHandler = WakeHandler.getInstance(entity.world).orElse(null);
        if (wakeHandler == null) return;

        for (WakeNode node : WakeNode.Factory.splashNodes(entity, (int) Math.floor(((ProducesWake) entity).wakes$wakeHeight()))) {
            wakeHandler.insert(node);
        }
    }

    private static int paddleLogCooldown = 0;

    public static void spawnPaddleSplashCloudParticle(World world, EntityBoat boat) {
        for (int i = 0; i < 2; i++) {
            if (boat.getPaddleState(i)) {
                double phase = boat.getRowingTime(i, 1.0f) % (2 * Math.PI);
                if (0.3926991f / 2 <= phase && phase <= 0.7853982f + 0.3926991f) {
                    Vec3d rot = boat.getLookVec();
                    double x = boat.posX + (i == 1 ? -rot.z : rot.z);
                    double z = boat.posZ + (i == 1 ? rot.x : -rot.x);
                    float wakeHeight = ((ProducesWake) boat).wakes$wakeHeight();
                    if (paddleLogCooldown <= 0) {
                        com.leclowndu93150.wakes.WakesMod.LOGGER.info("[PADDLE] boatY={} wakeHeight={} fluidLevel={}",
                            String.format("%.3f", boat.posY), String.format("%.3f", wakeHeight), String.format("%.3f", getFluidLevel(world, boat)));
                        paddleLogCooldown = 40;
                    }
                    paddleLogCooldown--;
                    Minecraft.getMinecraft().effectRenderer.addEffect(
                            new SplashCloudParticle(world, x, wakeHeight, z, 0, 0, 0));
                }
            }
        }
    }

    public static void spawnSplashPlane(World world, Entity owner) {
        SplashPlaneSpawner.spawn(world, owner);
    }

    public static void placeWakeTrail(Entity entity) {
        WakeHandler wakeHandler = WakeHandler.getInstance(entity.world).orElse(null);
        if (wakeHandler == null) return;

        ProducesWake producer = (ProducesWake) entity;
        double velocity = producer.wakes$getHorizontalVelocity();
        int y = (int) Math.floor(producer.wakes$wakeHeight());

        if (entity instanceof EntityBoat) {
            EntityBoat boat = (EntityBoat) entity;
            for (WakeNode node : WakeNode.Factory.rowingNodes(boat, y)) {
                wakeHandler.insert(node);
            }
            if (WakesConfig.spawnParticles) {
                WakesUtils.spawnPaddleSplashCloudParticle(entity.world, boat);
            }
        }

        Vec3d prevPos = producer.wakes$getPrevPos();
        if (prevPos == null) {
            return;
        }
        for (WakeNode node : WakeNode.Factory.thickNodeTrail(prevPos.x, prevPos.z, entity.posX, entity.posZ, y, WakesConfig.initialStrength, velocity, entity.width)) {
            wakeHandler.insert(node);
        }
    }

    public static EffectSpawningRule getEffectRuleFromSource(Entity source) {
        if (source == null) {
            return EffectSpawningRule.DISABLED;
        }

        if (source instanceof EntityBoat) {
            EntityBoat boat = (EntityBoat) source;
            List<Entity> passengers = boat.getPassengers();
            if (passengers.contains(Minecraft.getMinecraft().player)) {
                return WakesConfig.boatSpawning;
            }
            for (Entity passenger : passengers) {
                if (passenger instanceof EntityPlayer && passenger != Minecraft.getMinecraft().player) {
                    return WakesConfig.boatSpawning.mask(WakesConfig.otherPlayersSpawning);
                }
            }
            return WakesConfig.boatSpawning;
        }
        if (source instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) source;
            if (player.isSpectator()) {
                return EffectSpawningRule.DISABLED;
            }
            if (player instanceof EntityPlayerSP) {
                return WakesConfig.playerSpawning;
            }
            return WakesConfig.otherPlayersSpawning;
        }
        if (source instanceof EntityLivingBase) {
            return WakesConfig.mobSpawning;
        }
        if (source instanceof EntityItem) {
            return WakesConfig.itemSpawning;
        }
        return EffectSpawningRule.DISABLED;
    }

    public static void bresenhamLine(int x1, int y1, int x2, int y2, ArrayList<Long> points) {
        int dy = y2 - y1;
        int dx = x2 - x1;
        if (dx == 0) {
            if (y2 < y1) {
                int temp = y1;
                y1 = y2;
                y2 = temp;
            }
            for (int y = y1; y < y2 + 1; y++) {
                points.add(posAsLong(x1, y));
            }
        } else {
            float k = (float) dy / dx;
            int adjust = k >= 0 ? 1 : -1;
            int offset = 0;
            if (k <= 1 && k >= -1) {
                int delta = Math.abs(dy) * 2;
                int threshold = Math.abs(dx);
                int thresholdInc = Math.abs(dx) * 2;
                int y = y1;
                if (x2 < x1) {
                    int temp = x1;
                    x1 = x2;
                    x2 = temp;
                    y = y2;
                }
                for (int x = x1; x < x2 + 1; x++) {
                    points.add(posAsLong(x, y));
                    offset += delta;
                    if (offset >= threshold) {
                        y += adjust;
                        threshold += thresholdInc;
                    }
                }
            } else {
                int delta = Math.abs(dx) * 2;
                int threshold = Math.abs(dy);
                int thresholdInc = Math.abs(dy) * 2;
                int x = x1;
                if (y2 < y1) {
                    int temp = y1;
                    y1 = y2;
                    y2 = temp;
                }
                for (int y = y1; y < y2 + 1; y++) {
                    points.add(posAsLong(x, y));
                    offset += delta;
                    if (offset >= threshold) {
                        x += adjust;
                        threshold += thresholdInc;
                    }
                }
            }
        }
    }

    public static long posAsLong(int x, int y) {
        int xs = x >> 31 & 1;
        int ys = y >> 31 & 1;
        x &= ~(1 << 31);
        y &= ~(1 << 31);
        long pos = (long) x << 32 | y;
        pos ^= (-xs ^ pos) & (1L << 63);
        pos ^= (-ys ^ pos) & (1L << 31);
        return pos;
    }

    public static int[] longAsPos(long pos) {
        return new int[]{(int) (pos >> 32), (int) pos};
    }

    public static int[] abgrInt2rgbaArr(int n) {
        return new int[]{
            n & 0xFF,
            (n >> 8) & 0xFF,
            (n >> 16) & 0xFF,
            (n >> 24) & 0xFF
        };
    }

    public static int rgbaArr2abgrInt(int[] arr) {
        return (arr[0] & 0xFF) | ((arr[1] & 0xFF) << 8) | ((arr[2] & 0xFF) << 16) | ((arr[3] & 0xFF) << 24);
    }

    public static float getFluidLevel(World world, Entity entityInFluid) {
        AxisAlignedBB box = entityInFluid.getEntityBoundingBox();
        return getFluidLevel(world,
                MathHelper.floor(box.minX), MathHelper.ceil(box.maxX),
                MathHelper.floor(box.minY), MathHelper.ceil(box.maxY),
                MathHelper.floor(box.minZ), MathHelper.ceil(box.maxZ));
    }

    private static float getFluidLevel(World world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        yLoop:
        for (int y = minY; y < maxY; ++y) {
            float f = 0.0f;
            for (int x = minX; x < maxX; ++x) {
                for (int z = minZ; z < maxZ; ++z) {
                    blockPos.setPos(x, y, z);
                    IBlockState state = world.getBlockState(blockPos);
                    if (state.getMaterial() == Material.WATER) {
                        Block block = state.getBlock();
                        boolean isSource;
                        if (block instanceof BlockLiquid) {
                            isSource = state.getValue(BlockLiquid.LEVEL) == 0;
                        } else if (block instanceof BlockFluidBase) {
                            isSource = state.getValue(BlockFluidBase.LEVEL) == ((BlockFluidBase) block).getMaxRenderHeightMeta();
                        } else {
                            isSource = true;
                        }
                        if (isSource) {
                            float height = BlockLiquid.getLiquidHeightPercent(0);
                            f = Math.max(f, height);
                        }
                    }
                    if (f >= 1.0f) continue yLoop;
                }
            }
            if (!(f < 1.0f)) continue;
            return blockPos.getY() + 1 - f;
        }
        return maxY + 1;
    }

    public static class SplashPlaneSpawner {
        public static void spawn(World world, Entity owner) {
            float wakeY = getFluidLevel(world, owner);
            com.leclowndu93150.wakes.WakesMod.LOGGER.info("[SPLASH_PLANE_SPAWN] ownerY={} fluidLevel={} wakeHeight={}",
                String.format("%.3f", owner.posY), String.format("%.3f", wakeY),
                String.format("%.3f", ((ProducesWake) owner).wakes$wakeHeight()));
            com.leclowndu93150.wakes.particle.custom.SplashPlaneParticle particle =
                    new com.leclowndu93150.wakes.particle.custom.SplashPlaneParticle(world, owner.posX, wakeY, owner.posZ);
            particle.owner = owner;
            particle.yaw = particle.prevYaw = owner.rotationYaw;
            ((ProducesWake) owner).wakes$setSplashPlane(particle);
            Minecraft.getMinecraft().effectRenderer.addEffect(particle);
        }
    }
}
