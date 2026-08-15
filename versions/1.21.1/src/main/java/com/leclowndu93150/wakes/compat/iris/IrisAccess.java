package com.leclowndu93150.wakes.compat.iris;

import net.neoforged.fml.ModList;

public final class IrisAccess {

    private static final IrisHooks HOOKS =
            ModList.get().isLoaded("iris") ? new LoadedIrisHooks() : new NoIrisHooks();

    private IrisAccess() {}

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
