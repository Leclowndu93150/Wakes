package com.leclowndu93150.wakes;

import net.fabricmc.api.ClientModInitializer;

public class WakesClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WakesClient.init();
    }
}
