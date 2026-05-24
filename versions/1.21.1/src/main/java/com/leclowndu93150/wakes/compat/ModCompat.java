package com.leclowndu93150.wakes.compat;

import net.neoforged.fml.ModList;

public class ModCompat {
    private static final boolean SABLE_LOADED = ModList.get().isLoaded("sable");

    public static boolean isSableLoaded() {
        return SABLE_LOADED;
    }
}
