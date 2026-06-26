package com.example.speedsterpathwalk.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class PathPacketCodec {
    public static final int MAX_PACKET_NODES = 4096;

    private PathPacketCodec() {
    }

    public static void writePath(PacketByteBuf buf, List<BlockPos> path) {
        int count = Math.min(path.size(), MAX_PACKET_NODES);
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            buf.writeBlockPos(path.get(i));
        }
    }

    public static List<BlockPos> readPath(PacketByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_PACKET_NODES) {
            throw new IllegalArgumentException("Invalid path node count: " + count);
        }

        List<BlockPos> path = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            path.add(buf.readBlockPos());
        }
        return path;
    }
}
