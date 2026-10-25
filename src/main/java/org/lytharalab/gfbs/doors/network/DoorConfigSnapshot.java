package org.lytharalab.gfbs.doors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record DoorConfigSnapshot(BlockPos pos, String doorId, int mode, boolean manual) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(doorId, 64);
        buf.writeByte(mode);
        buf.writeBoolean(manual);
    }

    public static DoorConfigSnapshot decode(FriendlyByteBuf buf) {
        return new DoorConfigSnapshot(buf.readBlockPos(), buf.readUtf(64), buf.readByte(), buf.readBoolean());
    }
}
