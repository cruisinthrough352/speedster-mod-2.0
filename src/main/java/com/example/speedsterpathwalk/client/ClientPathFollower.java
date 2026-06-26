package com.example.speedsterpathwalk.client;

import com.example.speedsterpathwalk.SpeedsterPathwalkMod;
import com.example.speedsterpathwalk.network.PathPacketCodec;
import com.example.speedsterpathwalk.mixin.LivingEntityAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LimbAnimator;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public final class ClientPathFollower {
    private static final float SPRINT_LIMB_SPEED = 1.0F;
    private static final float SPRINT_LIMB_MULTIPLIER = 1.0F;

    private static boolean animationAssistActive = false;

    private ClientPathFollower() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SpeedsterPathwalkMod.START_PATH_PACKET, (client, handler, buf, responseSender) -> {
            List<BlockPos> path = PathPacketCodec.readPath(buf);
            client.execute(() -> start(client, path));
        });

        ClientPlayNetworking.registerGlobalReceiver(SpeedsterPathwalkMod.STOP_PATH_PACKET, (client, handler, buf, responseSender) -> client.execute(() -> stop(client, "Speedwalk controls released.")));

        ClientTickEvents.END_CLIENT_TICK.register(ClientPathFollower::tickAnimationAssist);
    }

    private static void start(MinecraftClient client, List<BlockPos> path) {
        animationAssistActive = true;
        pressForwardSprintForAnimation(client);
        if (client.player != null) {
            client.player.sendMessage(Text.literal("5x speedwalk path received. Server is controlling movement. Nodes: " + path.size()), true);
        }
    }

    private static void stop(MinecraftClient client, String message) {
        animationAssistActive = false;
        releaseMovementKeys(client);
        if (message != null && client.player != null) {
            client.player.sendMessage(Text.literal(message), true);
        }
    }

    private static void tickAnimationAssist(MinecraftClient client) {
        if (!animationAssistActive) {
            return;
        }

        if (client.player == null || client.world == null) {
            animationAssistActive = false;
            releaseMovementKeys(client);
            return;
        }

        pressForwardSprintForAnimation(client);
    }

    private static void pressForwardSprintForAnimation(MinecraftClient client) {
        if (client == null || client.options == null || client.player == null) {
            return;
        }

        client.options.forwardKey.setPressed(true);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
        client.options.sprintKey.setPressed(true);

        ClientPlayerEntity player = client.player;
        player.setSprinting(true);
        if (player.input != null) {
            player.input.pressingForward = true;
            player.input.pressingBack = false;
            player.input.pressingLeft = false;
            player.input.pressingRight = false;
            player.input.jumping = false;
            player.input.sneaking = false;
            player.input.movementForward = 1.0F;
            player.input.movementSideways = 0.0F;
        }

        forceSprintLimbAnimation(player);
    }

    private static void forceSprintLimbAnimation(ClientPlayerEntity player) {
        LimbAnimator limbAnimator = ((LivingEntityAccessor) player).speedster_pathwalk$getLimbAnimator();
        limbAnimator.setSpeed(SPRINT_LIMB_SPEED);
        limbAnimator.updateLimbs(SPRINT_LIMB_SPEED, SPRINT_LIMB_MULTIPLIER);
    }

    private static void releaseMovementKeys(MinecraftClient client) {
        if (client == null || client.options == null) {
            return;
        }
        client.options.forwardKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
        client.options.sprintKey.setPressed(false);

        if (client.player != null) {
            client.player.setSprinting(false);
            if (client.player.input != null) {
                client.player.input.pressingForward = false;
                client.player.input.pressingBack = false;
                client.player.input.pressingLeft = false;
                client.player.input.pressingRight = false;
                client.player.input.jumping = false;
                client.player.input.sneaking = false;
                client.player.input.movementForward = 0.0F;
                client.player.input.movementSideways = 0.0F;
            }
        }
    }
}
