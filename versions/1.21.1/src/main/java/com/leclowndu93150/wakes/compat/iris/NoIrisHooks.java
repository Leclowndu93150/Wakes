package com.leclowndu93150.wakes.compat.iris;

final class NoIrisHooks implements IrisHooks {

    @Override
    public boolean shadersEnabled() {
        return false;
    }

    @Override
    public String packName() {
        return "";
    }

    @Override
    public int optionsRevision() {
        return -1;
    }

    @Override
    public boolean hasOption(String name) {
        return false;
    }

    @Override
    public boolean flag(String name, boolean fallback) {
        return fallback;
    }

    @Override
    public float number(String name, float fallback) {
        return fallback;
    }
}
