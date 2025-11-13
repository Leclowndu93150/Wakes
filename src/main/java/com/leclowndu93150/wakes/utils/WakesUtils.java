package com.leclowndu93150.wakes.utils;

import com.alekiponi.alekiships.common.entity.vehicle.AbstractAlekiBoatEntity;
import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.compat.alekiships.AlekiShipsCompat;
import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.config.enums.EffectSpawningRule;
import com.leclowndu93150.wakes.duck.ProducesWake;
import com.leclowndu93150.wakes.particle.ModParticles;
import com.leclowndu93150.wakes.particle.WithOwnerParticleType;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.simulation.WakeNode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WakesUtils {

    public static void placeFallSplash(Entity entity) {
        WakeHandler wakeHandler = WakeHandler.getInstance(entity.level()).orElse(null);
        if (wakeHandler == null) return;

        for (WakeNode node : WakeNode.Factory.splashNodes(entity, (int) Math.floor(((ProducesWake) entity).wakes$wakeHeight()))) {
            wakeHandler.insert(node);
        }
    }

    public static void spawnPaddleSplashCloudParticle(Level world, Boat boat) {
        // TODO MORE OBJECT ORIENTED APPROACH TO PARTICLE SPAWNING
        for (int i = 0; i < 2; i++) {
            if (boat.getPaddleState(i)) {
                double phase = boat.paddlePositions[i] % (2*Math.PI);
                if (Boat.PADDLE_SPEED / 2 <= phase && phase <= Boat.PADDLE_SOUND_TIME + Boat.PADDLE_SPEED) {
                    Vec3 rot = boat.getViewVector(1.0f);
                    double x = boat.getX() + (i == 1 ? -rot.z : rot.z);
                    double z = boat.getZ() + (i == 1 ? rot.x : -rot.x);
                    Vec3 pos = new Vec3(x, ((ProducesWake) boat).wakes$wakeHeight(), z);
                    world.addParticle(ModParticles.SPLASH_CLOUD.get(), pos.x, pos.y, pos.z, 0, 0, 0);
                }
            }
        }
    }

    public static void spawnSplashPlane(Level world, Entity owner) {
        WithOwnerParticleType wake = ModParticles.SPLASH_PLANE.get().withOwner(owner);
        Vec3 pos = owner.position();
        world.addParticle(wake, pos.x, pos.y, pos.z, 0, 0, 0);
    }

    public static void placeWakeTrail(Entity entity) {
        WakeHandler wakeHandler = WakeHandler.getInstance(entity.level()).orElse(null);
        if (wakeHandler == null) return;

        ProducesWake producer = (ProducesWake) entity;
        double velocity = producer.wakes$getHorizontalVelocity();
        int y = (int) Math.floor(producer.wakes$wakeHeight());

        if (entity instanceof Boat boat) {
            for (WakeNode node : WakeNode.Factory.rowingNodes(boat, y)) {
                wakeHandler.insert(node);
            }
            if (WakesConfig.APPEARANCE.spawnParticles.get()) {
                WakesUtils.spawnPaddleSplashCloudParticle(entity.level(), boat);
            }
        } else if (AlekiShipsCompat.isAlekiShipsBoat(entity)) {
            spawnAlekiShipsWakes(entity, wakeHandler, y);
        }
      
        Vec3 prevPos = producer.wakes$getPrevPos();
        if (prevPos == null) {
            return;
        }
        for (WakeNode node : WakeNode.Factory.thickNodeTrail(prevPos.x, prevPos.z, entity.getX(), entity.getZ(), y, WakesConfig.GENERAL.initialStrength.get(), velocity, entity.getBbWidth())) {
            wakeHandler.insert(node);
        }
    }

    private static void spawnAlekiShipsWakes(Entity entity, WakeHandler wakeHandler, int y) {
        try {
            if (AlekiShipsCompat.isAlekiShipsLoaded()) {
                AbstractAlekiBoatEntity boat = (AbstractAlekiBoatEntity) entity;
                ProducesWake producer = (ProducesWake) entity;
                float wakeHeight = producer.wakes$wakeHeight();
                
                AlekiShipsCompat.PaddleInfo paddleInfo = AlekiShipsCompat.getPaddleInfo(boat, 1.0f, wakeHeight);
                
                double velocity = boat.getDeltaMovement().horizontalDistance();
                int wakeY = (int) Math.floor(wakeHeight);
                
                for (int i = 0; i < paddleInfo.positions().length; i++) {
                    if (paddleInfo.inWater()[i]) {
                        Vec3 pos = paddleInfo.positions()[i];
                        Vec3 dir = Vec3.directionFromRotation(0, boat.getYRot()).scale(velocity);
                        Vec3 from = pos.subtract(dir.scale(1));
                        Vec3 to = pos.add(dir.scale(1));
                        
                        for (WakeNode node : WakeNode.Factory.nodeTrail(from.x, from.z, to.x, to.z, wakeY, WakesConfig.GENERAL.paddleStrength.get(), velocity)) {
                            wakeHandler.insert(node);
                        }
                        
                        if (WakesConfig.APPEARANCE.spawnParticles.get()) {
                            entity.level().addParticle(ModParticles.SPLASH_CLOUD.get(), 
                                pos.x, pos.y, pos.z, 0, 0, 0);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    public static EffectSpawningRule getEffectRuleFromSource(Entity source) {
        if (source == null) {
            return EffectSpawningRule.DISABLED;
        }
        if (WakesConfig.getMobBlacklist().contains(source.getType())) {
            return EffectSpawningRule.DISABLED;
        }
        
        if (AlekiShipsCompat.isAlekiShipsBoat(source)) {
            List<Entity> passengers = source.getPassengers();
            if (passengers.contains(Minecraft.getInstance().player)) {
                return WakesConfig.GENERAL.boatSpawning.get();
            }
            if (passengers.stream().anyMatch(Entity::isAlwaysTicking)) {
                return WakesConfig.GENERAL.boatSpawning.get().mask(WakesConfig.GENERAL.otherPlayersSpawning.get());
            }
            return WakesConfig.GENERAL.boatSpawning.get();
        }
        
        if (source instanceof Boat boat) {
            List<Entity> passengers = boat.getPassengers();
            if (passengers.contains(Minecraft.getInstance().player)) {
                return WakesConfig.GENERAL.boatSpawning.get();
            }
            if (passengers.stream().anyMatch(Entity::isAlwaysTicking)) {
                return WakesConfig.GENERAL.boatSpawning.get().mask(WakesConfig.GENERAL.otherPlayersSpawning.get());
            }
            return WakesConfig.GENERAL.boatSpawning.get();
        }
        if (source instanceof Player player) {
            if (player.isSpectator()) {
                return EffectSpawningRule.DISABLED;
            }
            if (player instanceof LocalPlayer) {
                return WakesConfig.GENERAL.playerSpawning.get();
            }
            if (player instanceof RemotePlayer) {
                return WakesConfig.GENERAL.otherPlayersSpawning.get();
            }
            return EffectSpawningRule.DISABLED;
        }
        if (source instanceof LivingEntity) {
            return WakesConfig.GENERAL.mobSpawning.get();
        }
        if (source instanceof ItemEntity) {
            return WakesConfig.GENERAL.itemSpawning.get();
        }
        return EffectSpawningRule.DISABLED;
    }

    public static void bresenhamLine(int x1, int y1, int x2, int y2, ArrayList<Long> points) {
        // https://www.youtube.com/watch?v=IDFB5CDpLDE credit
        // and of course Bresenham himself :P
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
        return new int[] {(int) (pos >> 32), (int) pos};
    }

    public static MutableComponent translatable(String ... subKeys) {
        StringBuilder translationKey = new StringBuilder(WakesClient.MOD_ID);
        for (String s : subKeys) {
           translationKey.append(".").append(s);
        }
        return Component.translatable(translationKey.toString());
    }

    public static int[] abgrInt2rgbaArr(int n) {
        int[] arr = new int[4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 8; j++) {
                arr[i] |= (n >> i*8+j & 1) << 7-j;
            }
        }
        return arr;
    }

    public static int rgbaArr2abgrInt(int[] arr) {
        int n = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 8; j++) {
                n |= (arr[i] >> j & 1) << i*8+j;
            }
        }
        return n;
    }

    // public static float getFluidColor() {
    //     return
    // }

    public static float getFluidLevel(Level world, Entity entityInFluid) {
        AABB box = entityInFluid.getBoundingBox();
        return getFluidLevel(world,
                Mth.floor(box.minX), Mth.ceil(box.maxX),
                Mth.floor(box.minY), Mth.ceil(box.maxY),
                Mth.floor(box.minZ), Mth.ceil(box.maxZ));
    }

//    public static float getWaterLevel(ModelPart.Cuboid cuboidInWater) {
//        return getWaterLevel(
//                (int) cuboidInWater.minX, (int) cuboidInWater.maxX,
//                (int) cuboidInWater.minY, (int) cuboidInWater.maxY,
//                (int) cuboidInWater.minZ, (int) cuboidInWater.maxZ);
//    }

    private static float getFluidLevel(Level world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        // Taken from BoatEntity$getWaterHeightBelow
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        yLoop:
        for (int y = minY; y < maxY; ++y) {
            float f = 0.0f;
            for (int x = minX; x < maxX; ++x) {
                for (int z = minZ; z < maxZ; ++z) {
                    blockPos.set(x, y, z);
                    FluidState fluidState = world.getFluidState(blockPos);
                    if (WakesConfig.getFluidWhitelist().contains(fluidState.getType()) && fluidState.isSource()) {
                        f = Math.max(f, fluidState.getHeight(world, blockPos));
                    }
                    if (f >= 1.0f) continue yLoop;
                }
            }
            if (!(f < 1.0f)) continue;
            return blockPos.getY() + f;
        }
        return maxY + 1;

    }
}
