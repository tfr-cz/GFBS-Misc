package org.lytharalab.cabinelevator.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record SaveControllerPacket(BlockPos pos, String channel, String floorName, int timeTicks) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(channel, 64);
        buf.writeUtf(floorName, 64);
        buf.writeVarInt(timeTicks);
    }

    public static SaveControllerPacket decode(FriendlyByteBuf buf) {
        return new SaveControllerPacket(buf.readBlockPos(), buf.readUtf(64), buf.readUtf(64), buf.readVarInt());
    }
}
