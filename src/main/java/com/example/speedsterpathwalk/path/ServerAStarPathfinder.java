package com.example.speedsterpathwalk.path;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

public final class ServerAStarPathfinder {
    public static final int DEFAULT_MAX_VISITED_NODES = 16000;
    public static final int DEFAULT_MAX_RADIUS_BLOCKS = 512;
    public static final int DEFAULT_MAX_PATH_NODES = 4096;
    public static final int DEFAULT_TARGET_FALLBACK_RADIUS_BLOCKS = 24;

    private static final int[][] HORIZONTAL_DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    private ServerAStarPathfinder() {
    }

    public static BlockPos resolveSurfaceTarget(ServerWorld world, int x, int z) {
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }

    public static BlockPos normalizeToStandable(ServerWorld world, BlockPos requested) {
        BlockPos clamped = clampToWorldHeight(world, requested);
        if (isStandable(world, clamped)) {
            return clamped;
        }

        for (int offset = 1; offset <= 8; offset++) {
            BlockPos up = clamped.up(offset);
            if (isWithinWalkableHeight(world, up) && isStandable(world, up)) {
                return up;
            }

            BlockPos down = clamped.down(offset);
            if (isWithinWalkableHeight(world, down) && isStandable(world, down)) {
                return down;
            }
        }

        return clamped;
    }

    public static PathResult findPath(ServerWorld world, BlockPos rawStart, BlockPos rawTarget) {
        BlockPos start = normalizeToStandable(world, rawStart);
        TargetResolution targetResolution = resolveNearestStandableTarget(world, rawTarget);
        BlockPos target = targetResolution.target();
        boolean targetIsStandable = isStandable(world, target);

        if (!isStandable(world, start)) {
            return PathResult.failure("The starting position is not walkable.", 0);
        }

        int horizontalDistance = Math.max(
                Math.abs(start.getX() - target.getX()),
                Math.abs(start.getZ() - target.getZ())
        );
        if (horizontalDistance > DEFAULT_MAX_RADIUS_BLOCKS) {
            return PathResult.failure("Target is too far away for this prototype. Maximum radius: " + DEFAULT_MAX_RADIUS_BLOCKS + " blocks.", 0);
        }

        PriorityQueue<Node> open = new PriorityQueue<>();
        Map<BlockPos, Node> nodes = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();

        Node startNode = new Node(start, null, 0.0D, estimateCost(start, target));
        open.add(startNode);
        nodes.put(start, startNode);

        Node closestReachable = startNode;
        double closestReachableScore = fallbackScore(start, target);

        int visited = 0;
        while (!open.isEmpty() && visited < DEFAULT_MAX_VISITED_NODES) {
            Node current = open.poll();
            if (closed.contains(current.pos)) {
                continue;
            }

            visited++;

            double currentScore = fallbackScore(current.pos, target);
            if (isBetterFallback(currentScore, current, closestReachableScore, closestReachable)) {
                closestReachable = current;
                closestReachableScore = currentScore;
            }

            if (targetIsStandable && current.pos.equals(target)) {
                List<BlockPos> path = reconstruct(current);
                if (path.size() > DEFAULT_MAX_PATH_NODES) {
                    return PathResult.failure("Path was found but is too long to send safely.", visited);
                }

                String message = targetResolution.exactRequestedTarget()
                        ? "Path found."
                        : "Exact target was not standable, so a path was found to the nearest standable target at " + formatPos(path.get(path.size() - 1)) + ".";
                return PathResult.success(message, simplify(path), visited, targetResolution.exactRequestedTarget());
            }

            closed.add(current.pos);

            for (BlockPos neighborPos : findNeighbors(world, current.pos)) {
                if (closed.contains(neighborPos)) {
                    continue;
                }

                double stepCost = movementCost(current.pos, neighborPos);
                double tentativeG = current.gCost + stepCost;

                Node known = nodes.get(neighborPos);
                if (known == null || tentativeG < known.gCost) {
                    Node next = new Node(neighborPos, current, tentativeG, tentativeG + estimateCost(neighborPos, target));
                    nodes.put(neighborPos, next);
                    open.add(next);
                }
            }
        }

        PathResult fallback = buildFallbackResult(start, closestReachable, target, visited, visited >= DEFAULT_MAX_VISITED_NODES);
        if (fallback != null) {
            return fallback;
        }

        if (visited >= DEFAULT_MAX_VISITED_NODES) {
            return PathResult.failure("No path found before the search safety limit was reached.", visited);
        }
        return PathResult.failure("No walkable path was found.", visited);
    }

    private static PathResult buildFallbackResult(BlockPos start, Node closestReachable, BlockPos target, int visited, boolean searchLimitReached) {
        if (closestReachable == null || closestReachable.pos.equals(start)) {
            return null;
        }

        List<BlockPos> path = reconstruct(closestReachable);
        if (path.size() > DEFAULT_MAX_PATH_NODES) {
            return PathResult.failure("Closest reachable path was found but is too long to send safely.", visited);
        }

        String reason = searchLimitReached
                ? "The exact target was not reached before the search safety limit."
                : "The exact target was not reachable by the walking pathfinder.";
        String message = reason + " Sending path to closest reachable point found: " + formatPos(path.get(path.size() - 1))
                + "; requested target was near " + formatPos(target) + ".";
        return PathResult.success(message, simplify(path), visited, false);
    }

    private static TargetResolution resolveNearestStandableTarget(ServerWorld world, BlockPos rawTarget) {
        BlockPos normalized = normalizeToStandable(world, rawTarget);
        if (isStandable(world, normalized)) {
            boolean exact = normalized.equals(rawTarget);
            return new TargetResolution(normalized, exact);
        }

        BlockPos clamped = clampToWorldHeight(world, rawTarget);
        BlockPos best = null;
        long bestScore = Long.MAX_VALUE;

        for (int radius = 0; radius <= DEFAULT_TARGET_FALLBACK_RADIUS_BLOCKS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    int x = clamped.getX() + dx;
                    int z = clamped.getZ() + dz;
                    BlockPos candidate = findBestStandableInColumn(world, x, clamped.getY(), z);
                    if (candidate == null) {
                        continue;
                    }

                    long score = targetFallbackScore(candidate, clamped);
                    if (score < bestScore) {
                        best = candidate;
                        bestScore = score;
                    }
                }
            }

            if (best != null) {
                return new TargetResolution(best, false);
            }
        }

        return new TargetResolution(clamped, false);
    }

    private static BlockPos findBestStandableInColumn(ServerWorld world, int x, int requestedY, int z) {
        BlockPos best = null;
        int bestVerticalDistance = Integer.MAX_VALUE;

        int minY = world.getBottomY() + 1;
        int maxY = (world.getBottomY() + world.getHeight()) - 2;
        int clampedY = MathHelper.clamp(requestedY, minY, maxY);

        for (int offset = 0; offset <= 32; offset++) {
            BlockPos up = new BlockPos(x, clampedY + offset, z);
            if (up.getY() <= maxY && isStandable(world, up)) {
                best = up;
                bestVerticalDistance = offset;
                break;
            }

            if (offset > 0) {
                BlockPos down = new BlockPos(x, clampedY - offset, z);
                if (down.getY() >= minY && isStandable(world, down)) {
                    best = down;
                    bestVerticalDistance = offset;
                    break;
                }
            }
        }

        BlockPos surface = resolveSurfaceTarget(world, x, z);
        if (isStandable(world, surface)) {
            int surfaceDistance = Math.abs(surface.getY() - clampedY);
            if (best == null || surfaceDistance < bestVerticalDistance) {
                best = surface;
            }
        }

        return best;
    }

    private static long targetFallbackScore(BlockPos candidate, BlockPos requested) {
        long dx = candidate.getX() - requested.getX();
        long dz = candidate.getZ() - requested.getZ();
        long dy = candidate.getY() - requested.getY();
        long horizontal = dx * dx + dz * dz;
        return horizontal * 10_000L + dy * dy;
    }

    private static BlockPos clampToWorldHeight(ServerWorld world, BlockPos requested) {
        int minY = world.getBottomY() + 1;
        int maxY = (world.getBottomY() + world.getHeight()) - 2;
        int y = MathHelper.clamp(requested.getY(), minY, maxY);
        return new BlockPos(requested.getX(), y, requested.getZ());
    }

    private static boolean isWithinWalkableHeight(ServerWorld world, BlockPos pos) {
        return pos.getY() > world.getBottomY() && pos.getY() < (world.getBottomY() + world.getHeight()) - 1;
    }

    private static boolean isBetterFallback(double score, Node candidate, double bestScore, Node best) {
        if (score < bestScore) {
            return true;
        }
        if (score > bestScore) {
            return false;
        }
        return best == null || candidate.gCost < best.gCost;
    }

    private static double fallbackScore(BlockPos from, BlockPos to) {
        double dx = from.getX() - to.getX();
        double dy = from.getY() - to.getY();
        double dz = from.getZ() - to.getZ();
        return (dx * dx) + (dz * dz) + (dy * dy * 0.25D);
    }

    private static boolean isCloseEnough(BlockPos pos, BlockPos target) {
        double dx = pos.getX() - target.getX();
        double dy = pos.getY() - target.getY();
        double dz = pos.getZ() - target.getZ();
        return (dx * dx + dy * dy + dz * dz) <= 1.5625D;
    }

    private static List<BlockPos> findNeighbors(ServerWorld world, BlockPos current) {
        List<BlockPos> neighbors = new ArrayList<>(HORIZONTAL_DIRECTIONS.length);

        for (int[] direction : HORIZONTAL_DIRECTIONS) {
            int nx = current.getX() + direction[0];
            int nz = current.getZ() + direction[1];

            BlockPos candidate = findStandableAtColumnNearY(world, nx, current.getY(), nz);
            if (candidate == null) {
                continue;
            }

            if (Math.abs(candidate.getY() - current.getY()) > 3) {
                continue;
            }

            if (isDiagonal(direction) && wouldCutCorner(world, current, direction, candidate.getY())) {
                continue;
            }

            neighbors.add(candidate);
        }

        return neighbors;
    }

    private static BlockPos findStandableAtColumnNearY(ServerWorld world, int x, int currentY, int z) {
        int minY = Math.max(world.getBottomY() + 1, currentY - 4);
        int maxY = Math.min((world.getBottomY() + world.getHeight()) - 2, currentY + 1);

        for (int y = maxY; y >= minY; y--) {
            BlockPos candidate = new BlockPos(x, y, z);
            if (isStandable(world, candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private static boolean isDiagonal(int[] direction) {
        return direction[0] != 0 && direction[1] != 0;
    }

    private static boolean wouldCutCorner(ServerWorld world, BlockPos current, int[] diagonal, int candidateY) {
        BlockPos sideA = new BlockPos(current.getX() + diagonal[0], candidateY, current.getZ());
        BlockPos sideB = new BlockPos(current.getX(), candidateY, current.getZ() + diagonal[1]);
        return !hasBodyRoom(world, sideA) || !hasBodyRoom(world, sideB);
    }

    public static boolean isStandable(ServerWorld world, BlockPos feetPos) {
        if (!world.getWorldBorder().contains(feetPos)) {
            return false;
        }

        if (!isWithinWalkableHeight(world, feetPos)) {
            return false;
        }

        if (!hasBodyRoom(world, feetPos) || !isSafeFluid(world, feetPos) || !isSafeFluid(world, feetPos.up())) {
            return false;
        }

        BlockPos groundPos = feetPos.down();
        return hasSolidGround(world, groundPos) || isRunnableWaterSurface(world, feetPos);
    }

    private static boolean hasSolidGround(ServerWorld world, BlockPos groundPos) {
        BlockState ground = world.getBlockState(groundPos);
        return !ground.getCollisionShape(world, groundPos).isEmpty();
    }

    private static boolean isRunnableWaterSurface(ServerWorld world, BlockPos feetPos) {
        BlockPos waterPos = feetPos.down();
        FluidState water = world.getFluidState(waterPos);
        FluidState feetFluid = world.getFluidState(feetPos);
        FluidState headFluid = world.getFluidState(feetPos.up());
        return water.isIn(FluidTags.WATER) && feetFluid.isEmpty() && headFluid.isEmpty();
    }

    private static boolean hasBodyRoom(ServerWorld world, BlockPos feetPos) {
        BlockState feet = world.getBlockState(feetPos);
        BlockState head = world.getBlockState(feetPos.up());
        return feet.getCollisionShape(world, feetPos).isEmpty()
                && head.getCollisionShape(world, feetPos.up()).isEmpty();
    }

    private static boolean isSafeFluid(ServerWorld world, BlockPos pos) {
        FluidState fluid = world.getFluidState(pos);
        return fluid.isEmpty() || fluid.isIn(FluidTags.WATER);
    }

    private static double estimateCost(BlockPos from, BlockPos to) {
        int dx = Math.abs(from.getX() - to.getX());
        int dz = Math.abs(from.getZ() - to.getZ());
        int diagonal = Math.min(dx, dz);
        int straight = Math.max(dx, dz) - diagonal;
        double horizontal = diagonal * 1.41421356237D + straight;
        double vertical = Math.max(0, to.getY() - from.getY()) * 0.75D;
        return horizontal + vertical;
    }

    private static double movementCost(BlockPos from, BlockPos to) {
        int dx = Math.abs(from.getX() - to.getX());
        int dz = Math.abs(from.getZ() - to.getZ());
        double horizontal = (dx == 1 && dz == 1) ? 1.41421356237D : 1.0D;
        double verticalPenalty = Math.max(0, to.getY() - from.getY()) * 0.75D;
        return horizontal + verticalPenalty;
    }

    private static List<BlockPos> reconstruct(Node node) {
        List<BlockPos> path = new ArrayList<>();
        Node current = node;
        while (current != null) {
            path.add(current.pos);
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private static List<BlockPos> simplify(List<BlockPos> path) {
        if (path.size() <= 2) {
            return path;
        }

        List<BlockPos> simplified = new ArrayList<>();
        simplified.add(path.get(0));

        int lastDx = Integer.signum(path.get(1).getX() - path.get(0).getX());
        int lastDy = Integer.signum(path.get(1).getY() - path.get(0).getY());
        int lastDz = Integer.signum(path.get(1).getZ() - path.get(0).getZ());

        for (int i = 2; i < path.size(); i++) {
            BlockPos previous = path.get(i - 1);
            BlockPos current = path.get(i);
            int dx = Integer.signum(current.getX() - previous.getX());
            int dy = Integer.signum(current.getY() - previous.getY());
            int dz = Integer.signum(current.getZ() - previous.getZ());

            if (dx != lastDx || dy != lastDy || dz != lastDz) {
                simplified.add(previous);
                lastDx = dx;
                lastDy = dy;
                lastDz = dz;
            }
        }

        simplified.add(path.get(path.size() - 1));
        return simplified;
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private record TargetResolution(BlockPos target, boolean exactRequestedTarget) {
    }

    private static final class Node implements Comparable<Node> {
        private final BlockPos pos;
        private final Node parent;
        private final double gCost;
        private final double fCost;

        private Node(BlockPos pos, Node parent, double gCost, double fCost) {
            this.pos = Objects.requireNonNull(pos);
            this.parent = parent;
            this.gCost = gCost;
            this.fCost = fCost;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.fCost, other.fCost);
        }
    }
}
