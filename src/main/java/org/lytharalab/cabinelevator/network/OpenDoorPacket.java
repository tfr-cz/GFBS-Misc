package org.lytharalab.cabinelevator.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record OpenDoorPacket(BlockPos controller) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(controller);
    }

    public static OpenDoorPacket decode(FriendlyByteBuf buf) {
        return new OpenDoorPacket(buf.readBlockPos());
    }
}
