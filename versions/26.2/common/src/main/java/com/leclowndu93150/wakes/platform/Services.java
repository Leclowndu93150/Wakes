package com.leclowndu93150.wakes.platform;

import java.util.ServiceLoader;

public final class Services {
    public static final WakesPlatform PLATFORM = load(WakesPlatform.class);

    private Services() {}

    private static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No service of " + clazz));
    }
}
