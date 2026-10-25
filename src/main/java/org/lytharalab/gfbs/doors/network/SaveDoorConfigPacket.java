package org.lytharalab.gfbs.doors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record SaveDoorConfigPacket(BlockPos pos, int mode, boolean manual) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(mode);
        buf.writeBoolean(manual);
    }

    public static SaveDoorConfigPacket decode(FriendlyByteBuf buf) {
        return new SaveDoorConfigPacket(buf.readBlockPos(), buf.readByte(), buf.readBoolean());
    }
}
