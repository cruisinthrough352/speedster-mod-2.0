package com.example.speedsterpathwalk;

import com.example.speedsterpathwalk.client.ClientPathFollower;
import net.fabricmc.api.ClientModInitializer;

public class SpeedsterPathwalkClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPathFollower.register();
    }
}
