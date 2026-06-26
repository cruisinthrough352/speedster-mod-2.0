package com.example.speedsterpathwalk.server;

import com.example.speedsterpathwalk.SpeedsterPathwalkMod;
import com.example.speedsterpathwalk.path.ServerAStarPathfinder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ServerSpeedwalkRunner {
    /**
     * Fixed 5x movement model.
     *
     * The server does not use the vanilla Speed effect and does not change the player's
     * movement-speed attribute. It advances five normal-sized path slices per server
     * tick. Version 0.2.1+ syncs every slice instead of only syncing the final tick
     * position, which makes the teleport-style movement look less chunky while keeping
     * the same total 5x travel rate.
     */
    public static final int FIXED_TIME_SCALE = 5;
    private static final double NORMAL_STEP_BLOCKS = 0.30D;
    private static final double NODE_REACH_DISTANCE = 0.08D;
    private static final double FINAL_REACH_DISTANCE = 0.20D;

    private static final int FALL_DAMAGE_GRACE_TICKS = 40;

    private static final Map<UUID, ActiveRun> ACTIVE_RUNS = new HashMap<>();
    private static final Map<UUID, Integer> FALL_DAMAGE_GRACE = new HashMap<>();

    private ServerSpeedwalkRunner() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ServerSpeedwalkRunner::tickServer);
    }

    public static void start(ServerPlayerEntity player, List<BlockPos> path) {
        if (path.isEmpty()) {
            player.sendMessage(Text.literal("Cannot start speedwalk: path is empty."), false);
            return;
        }

        List<Vec3d> points = new ArrayList<>(path.size());
        for (BlockPos pos : path) {
            points.add(toFeetCenter(pos));
        }

        int firstTarget = Math.min(1, points.size() - 1);
        ActiveRun run = new ActiveRun(player.getUuid(), player.getServerWorld(), points, firstTarget);
        ACTIVE_RUNS.put(player.getUuid(), run);
        FALL_DAMAGE_GRACE.remove(player.getUuid());
        player.fallDistance = 0.0F;
        player.setSprinting(true);
        player.sendMessage(Text.literal("5x speedwalk started. Nodes: " + points.size()), true);
    }

    public static boolean stop(ServerPlayerEntity player, String message) {
        boolean removed = ACTIVE_RUNS.remove(player.getUuid()) != null;
        if (removed) {
            player.setVelocity(Vec3d.ZERO);
            player.fallDistance = 0.0F;
            grantFallDamageGrace(player.getUuid());
            player.setSprinting(false);
            if (ServerPlayNetworking.canSend(player, SpeedsterPathwalkMod.STOP_PATH_PACKET)) {
                ServerPlayNetworking.send(player, SpeedsterPathwalkMod.STOP_PATH_PACKET, PacketByteBufs.empty());
            }
            if (message != null) {
                player.sendMessage(Text.literal(message), true);
            }
        }
        return removed;
    }

    public static boolean isFallDamageProtected(UUID playerId) {
        return ACTIVE_RUNS.containsKey(playerId) || FALL_DAMAGE_GRACE.getOrDefault(playerId, 0) > 0;
    }

    private static void grantFallDamageGrace(UUID playerId) {
        FALL_DAMAGE_GRACE.put(playerId, FALL_DAMAGE_GRACE_TICKS);
    }

    private static void tickFallDamageGrace() {
        if (FALL_DAMAGE_GRACE.isEmpty()) {
            return;
        }

        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : FALL_DAMAGE_GRACE.entrySet()) {
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                expired.add(entry.getKey());
            } else {
                entry.setValue(remaining);
            }
        }

        for (UUID uuid : expired) {
            FALL_DAMAGE_GRACE.remove(uuid);
        }
    }

    private static void tickServer(MinecraftServer server) {
        tickFallDamageGrace();

        if (ACTIVE_RUNS.isEmpty()) {
            return;
        }

        List<UUID> toRemove = new ArrayList<>();
        for (ActiveRun run : ACTIVE_RUNS.values()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(run.playerId);
            if (player == null || player.isRemoved() || !player.isAlive() || player.getServerWorld() != run.world) {
                toRemove.add(run.playerId);
                continue;
            }

            TickResult result = tickRun(player, run);
            if (result.finished()) {
                toRemove.add(run.playerId);
                Vec3d finalPos = resolveFinalLandingPosition(run.world, run.points.get(run.points.size() - 1));
                teleportPlayer(player, player.getPos(), finalPos, result.yaw());
                player.setVelocity(Vec3d.ZERO);
                player.fallDistance = 0.0F;
                grantFallDamageGrace(player.getUuid());
                player.setSprinting(false);
                player.sendMessage(Text.literal("Destination reached."), true);
                if (ServerPlayNetworking.canSend(player, SpeedsterPathwalkMod.STOP_PATH_PACKET)) {
                    ServerPlayNetworking.send(player, SpeedsterPathwalkMod.STOP_PATH_PACKET, PacketByteBufs.empty());
                }
            }
        }

        for (UUID uuid : toRemove) {
            ACTIVE_RUNS.remove(uuid);
        }
    }

    private static TickResult tickRun(ServerPlayerEntity player, ActiveRun run) {
        Vec3d oldPos = player.getPos();
        Vec3d simulatedPos = oldPos;
        float yaw = player.getYaw();

        player.setSprinting(true);
        player.fallDistance = 0.0F;

        for (int slice = 0; slice < FIXED_TIME_SCALE; slice++) {
            if (run.currentIndex >= run.points.size()) {
                return new TickResult(true, yaw);
            }

            MoveSlice sliceResult = advanceOneNormalMovementSlice(simulatedPos, run);
            simulatedPos = sliceResult.position();
            run.currentIndex = sliceResult.currentIndex();
            if (!Float.isNaN(sliceResult.yaw())) {
                yaw = sliceResult.yaw();
            }

            if (sliceResult.finished()) {
                Vec3d finalPos = resolveFinalLandingPosition(run.world, run.points.get(run.points.size() - 1));
                teleportPlayer(player, oldPos, finalPos, yaw);
                return new TickResult(true, yaw);
            }

            // In 0.2.0 the server only synced once per tick after all five slices.
            // In 0.2.1+ each slice is synced. The distance between visible position
            // corrections is smaller, so the movement still uses teleports but looks
            // less chunky.
            teleportPlayer(player, oldPos, simulatedPos, yaw);
            oldPos = simulatedPos;
        }

        return new TickResult(false, yaw);
    }

    private static MoveSlice advanceOneNormalMovementSlice(Vec3d from, ActiveRun run) {
        int index = run.currentIndex;
        Vec3d position = from;
        float yaw = Float.NaN;

        while (index < run.points.size()) {
            Vec3d target = run.points.get(index);
            boolean finalNode = index == run.points.size() - 1;
            double reachDistance = finalNode ? FINAL_REACH_DISTANCE : NODE_REACH_DISTANCE;
            Vec3d delta = target.subtract(position);
            double distance = delta.length();

            if (distance <= reachDistance) {
                if (finalNode) {
                    return new MoveSlice(target, index + 1, true, yaw);
                }
                index++;
                continue;
            }

            if (Math.abs(delta.x) > 0.0001D || Math.abs(delta.z) > 0.0001D) {
                yaw = (float) (Math.atan2(delta.z, delta.x) * (180.0D / Math.PI)) - 90.0F;
            }

            double step = Math.min(NORMAL_STEP_BLOCKS, Math.max(0.0D, distance - reachDistance));
            if (step <= 0.0001D) {
                if (finalNode) {
                    return new MoveSlice(target, index + 1, true, yaw);
                }
                index++;
                continue;
            }

            position = position.add(delta.multiply(step / distance));
            return new MoveSlice(position, index, false, yaw);
        }

        return new MoveSlice(position, index, true, yaw);
    }

    private static void teleportPlayer(ServerPlayerEntity player, Vec3d oldPos, Vec3d newPos, float yaw) {
        Vec3d tickVelocity = newPos.subtract(oldPos);
        player.setVelocity(tickVelocity);
        player.fallDistance = 0.0F;
        player.setSprinting(true);
        player.setYaw(yaw);
        player.setHeadYaw(yaw);
        player.setBodyYaw(yaw);
        player.networkHandler.requestTeleport(newPos.x, newPos.y, newPos.z, yaw, player.getPitch());
    }

    private static Vec3d resolveFinalLandingPosition(ServerWorld world, Vec3d desired) {
        BlockPos desiredFeet = BlockPos.ofFloored(desired.x, desired.y, desired.z);
        if (ServerAStarPathfinder.isStandable(world, desiredFeet)) {
            return toFeetCenter(desiredFeet);
        }

        for (int offset = 1; offset <= 3; offset++) {
            BlockPos up = desiredFeet.up(offset);
            if (ServerAStarPathfinder.isStandable(world, up)) {
                return toFeetCenter(up);
            }

            BlockPos down = desiredFeet.down(offset);
            if (ServerAStarPathfinder.isStandable(world, down)) {
                return toFeetCenter(down);
            }
        }

        return desired;
    }

    private static Vec3d toFeetCenter(BlockPos pos) {
        return new Vec3d(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
    }

    private static final class ActiveRun {
        private final UUID playerId;
        private final ServerWorld world;
        private final List<Vec3d> points;
        private int currentIndex;

        private ActiveRun(UUID playerId, ServerWorld world, List<Vec3d> points, int currentIndex) {
            this.playerId = playerId;
            this.world = world;
            this.points = points;
            this.currentIndex = currentIndex;
        }
    }

    private record MoveSlice(Vec3d position, int currentIndex, boolean finished, float yaw) {
    }

    private record TickResult(boolean finished, float yaw) {
    }
}
