package com.example.speedsterpathwalk.command;

import com.example.speedsterpathwalk.SpeedsterPathwalkMod;
import com.example.speedsterpathwalk.network.PathPacketCodec;
import com.example.speedsterpathwalk.path.PathResult;
import com.example.speedsterpathwalk.path.ServerAStarPathfinder;
import com.example.speedsterpathwalk.server.ServerSpeedwalkRunner;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class SpeedwalkCommand {
    private SpeedwalkCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                CommandManager.literal("speedwalk")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("stop")
                                .executes(context -> stopWalking(context.getSource())))
                        .then(CommandManager.literal("block")
                                .then(CommandManager.argument("x", IntegerArgumentType.integer())
                                        .then(CommandManager.argument("y", IntegerArgumentType.integer())
                                                .then(CommandManager.argument("z", IntegerArgumentType.integer())
                                                        .executes(context -> {
                                                            int x = IntegerArgumentType.getInteger(context, "x");
                                                            int y = IntegerArgumentType.getInteger(context, "y");
                                                            int z = IntegerArgumentType.getInteger(context, "z");
                                                            return walkToBlock(context.getSource(), new BlockPos(x, y, z));
                                                        })))))
                        .then(CommandManager.argument("x", IntegerArgumentType.integer())
                                .then(CommandManager.argument("z", IntegerArgumentType.integer())
                                        .executes(context -> {
                                            int x = IntegerArgumentType.getInteger(context, "x");
                                            int z = IntegerArgumentType.getInteger(context, "z");
                                            return walkToXZ(context.getSource(), x, z);
                                        })))
        ));
    }

    private static int walkToXZ(ServerCommandSource source, int x, int z) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld world = player.getServerWorld();
        BlockPos target = ServerAStarPathfinder.resolveSurfaceTarget(world, x, z);
        return walkToBlock(source, target);
    }

    private static int walkToBlock(ServerCommandSource source, BlockPos target) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld world = player.getServerWorld();
        BlockPos start = player.getBlockPos();

        source.sendFeedback(() -> Text.literal("Searching for path to " + formatPos(target) + "..."), false);

        PathResult result = ServerAStarPathfinder.findPath(world, start, target);
        if (!result.success()) {
            source.sendError(Text.literal(result.message() + " Visited nodes: " + result.visitedNodes()));
            return 0;
        }

        if (!ServerPlayNetworking.canSend(player, SpeedsterPathwalkMod.START_PATH_PACKET)) {
            source.sendError(Text.literal("That client has not registered the Speedster Pathwalk receiver. Install this mod on the client too, then reconnect."));
            return 0;
        }

        PacketByteBuf buf = PacketByteBufs.create();
        PathPacketCodec.writePath(buf, result.path());
        ServerPlayNetworking.send(player, SpeedsterPathwalkMod.START_PATH_PACKET, buf);
        ServerSpeedwalkRunner.start(player, result.path());

        source.sendFeedback(() -> Text.literal(result.message() + " Nodes: " + result.path().size() + ", searched: " + result.visitedNodes() + ", speed: 5x"), false);
        return result.path().size();
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private static int stopWalking(ServerCommandSource source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        boolean stopped = ServerSpeedwalkRunner.stop(player, "Speedwalk stopped.");
        if (!stopped && ServerPlayNetworking.canSend(player, SpeedsterPathwalkMod.STOP_PATH_PACKET)) {
            ServerPlayNetworking.send(player, SpeedsterPathwalkMod.STOP_PATH_PACKET, PacketByteBufs.empty());
        }
        source.sendFeedback(() -> Text.literal(stopped ? "Stopped 5x speedwalk." : "No active speedwalk was running."), false);
        return 1;
    }
}
