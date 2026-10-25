package org.lytharalab.gfbs.doors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record DoorTogglePacket(BlockPos pos) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public static DoorTogglePacket decode(FriendlyByteBuf buf) {
        return new DoorTogglePacket(buf.readBlockPos());
    }
}
