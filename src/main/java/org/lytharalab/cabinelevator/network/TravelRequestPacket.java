package org.lytharalab.cabinelevator.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record TravelRequestPacket(BlockPos controller, BlockPos target) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(controller);
        buf.writeBlockPos(target);
    }

    public static TravelRequestPacket decode(FriendlyByteBuf buf) {
        return new TravelRequestPacket(buf.readBlockPos(), buf.readBlockPos());
    }
}
