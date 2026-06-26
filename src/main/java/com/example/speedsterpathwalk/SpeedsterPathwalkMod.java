package com.example.speedsterpathwalk;

import com.example.speedsterpathwalk.command.SpeedwalkCommand;
import com.example.speedsterpathwalk.server.ServerSpeedwalkRunner;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class SpeedsterPathwalkMod implements ModInitializer {
    public static final String MOD_ID = "speedster_pathwalk";
    public static final Identifier START_PATH_PACKET = new Identifier(MOD_ID, "start_path");
    public static final Identifier STOP_PATH_PACKET = new Identifier(MOD_ID, "stop_path");

    @Override
    public void onInitialize() {
        SpeedwalkCommand.register();
        ServerSpeedwalkRunner.register();
    }
}
