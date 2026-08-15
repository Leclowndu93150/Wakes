package com.leclowndu93150.wakes.compat.iris;

public interface IrisHooks {
    boolean shadersEnabled();

    String packName();

    int optionsRevision();

    boolean hasOption(String name);

    boolean flag(String name, boolean fallback);

    float number(String name, float fallback);
}
