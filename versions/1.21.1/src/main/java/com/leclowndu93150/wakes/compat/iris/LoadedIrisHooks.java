package com.leclowndu93150.wakes.compat.iris;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.option.OptionSet;
import net.irisshaders.iris.shaderpack.option.values.OptionValues;

final class LoadedIrisHooks implements IrisHooks {

    @Override
    public boolean shadersEnabled() {
        return IrisApi.getInstance().isShaderPackInUse();
    }

    @Override
    public String packName() {
        String name = Iris.getCurrentPackName();
        return name == null ? "" : name;
    }

    @Override
    public int optionsRevision() {
        OptionValues values = optionValues();
        return values == null ? -1 : values.getOptionsChanged();
    }

    @Override
    public boolean hasOption(String name) {
        OptionSet options = optionSet();
        return options != null && (options.isBooleanOption(name) || options.getStringOptions().containsKey(name));
    }

    @Override
    public boolean flag(String name, boolean fallback) {
        OptionSet options = optionSet();
        if (options == null || !options.isBooleanOption(name)) return fallback;
        return optionValues().getBooleanValueOrDefault(name);
    }

    @Override
    public float number(String name, float fallback) {
        OptionSet options = optionSet();
        if (options == null || !options.getStringOptions().containsKey(name)) return fallback;
        try {
            return Float.parseFloat(optionValues().getStringValueOrDefault(name).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private OptionSet optionSet() {
        ShaderPack pack = currentPack();
        return pack == null ? null : pack.getShaderPackOptions().getOptionSet();
    }

    private OptionValues optionValues() {
        ShaderPack pack = currentPack();
        return pack == null ? null : pack.getShaderPackOptions().getOptionValues();
    }

    private ShaderPack currentPack() {
        return Iris.getCurrentPack().orElse(null);
    }
}
