package com.leclowndu93150.wakes.compat.iris;

import net.minecraftforge.fml.ModList;

public final class IrisAccess {

    private static final IrisHooks HOOKS = resolve();

    private IrisAccess() {}

    private static IrisHooks resolve() {
        ModList mods = ModList.get();
        return mods.isLoaded("oculus") || mods.isLoaded("iris") ? new LoadedIrisHooks() : new NoIrisHooks();
    }

    public static boolean shadersEnabled() {
        return HOOKS.shadersEnabled();
    }

    public static String packName() {
        return HOOKS.packName();
    }

    public static int optionsRevision() {
        return HOOKS.optionsRevision();
    }

    public static boolean hasOption(String name) {
        return HOOKS.hasOption(name);
    }

    public static boolean flag(String name, boolean fallback) {
        return HOOKS.flag(name, fallback);
    }

    public static float number(String name, float fallback) {
        return HOOKS.number(name, fallback);
    }
}
