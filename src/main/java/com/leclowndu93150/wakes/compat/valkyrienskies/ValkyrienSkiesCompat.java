package com.leclowndu93150.wakes.compat.valkyrienskies;

import com.leclowndu93150.wakes.duck.ProducesWake;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.loading.FMLLoader;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayList;

public class ValkyrienSkiesCompat {
    private static Boolean vs2Loaded = null;
    private static ValkyrienSkiesCompat instance;
    
    private int shipSizeUpdaterCooldown = 0;
    private int currentShipIndex = 0;
    private ArrayList<Ship> ships = new ArrayList<>();
    private static double seaLevel = 62.9;

    public static boolean isVS2Loaded() {
        if (vs2Loaded == null) {
            vs2Loaded = FMLLoader.getLoadingModList().getModFileById("valkyrienskies") != null;
        }
        return vs2Loaded;
    }
    
    public static ValkyrienSkiesCompat getInstance() {
        if (instance == null) {
            instance = new ValkyrienSkiesCompat();
        }
        return instance;
    }

    public void onClientTick() {
        if (Minecraft.getInstance().player == null) return;

        Level level = Minecraft.getInstance().player.level();
        
        if (seaLevel != 63 && level != null) {
            seaLevel = level.getSeaLevel() - 0.1;
        }
        ships.clear();
        ships.addAll(VSGameUtilsKt.getAllShips(level));

        if (ships.isEmpty()) return;

        if (shipSizeUpdaterCooldown == 0) {
            if (currentShipIndex >= ships.size()) {
                currentShipIndex = 0;
            }
            ShipWake.checkShipSize(ships.get(currentShipIndex));
            currentShipIndex++;
        }

        ships.forEach(s -> {
            if (s != null && ((DynamicWakeSize)s).getWidth() > 0) {
                ShipWake.placeWakeTrail(s);
                ((ProducesWake)s).wakes$setPrevPos(((DynamicWakeSize)s).getPos());
            }
        });

        if (shipSizeUpdaterCooldown >= (int)Math.max(9/ships.size(), 1)) {
            shipSizeUpdaterCooldown = 0;
        } else {
            shipSizeUpdaterCooldown++;
        }
    }

    public static double getSeaLevel() {
        return seaLevel;
    }
}
