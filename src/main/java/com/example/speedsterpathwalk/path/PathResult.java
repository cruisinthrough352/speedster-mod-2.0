package com.example.speedsterpathwalk.path;

import net.minecraft.util.math.BlockPos;

import java.util.List;

public record PathResult(boolean success, String message, List<BlockPos> path, int visitedNodes, boolean exactTarget) {
    public static PathResult success(List<BlockPos> path, int visitedNodes) {
        return new PathResult(true, "Path found.", path, visitedNodes, true);
    }

    public static PathResult success(String message, List<BlockPos> path, int visitedNodes, boolean exactTarget) {
        return new PathResult(true, message, path, visitedNodes, exactTarget);
    }

    public static PathResult failure(String message, int visitedNodes) {
        return new PathResult(false, message, List.of(), visitedNodes, false);
    }
}
