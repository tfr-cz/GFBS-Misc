package org.lytharalab.gfbs.doors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record SaveDoorConfigPacket(BlockPos pos, int mode, boolean manual, int extraHeight) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeByte(this.mode);
        buf.writeBoolean(this.manual);
        buf.writeVarInt(this.extraHeight);
    }

    public static SaveDoorConfigPacket decode(FriendlyByteBuf buf) {
        return new SaveDoorConfigPacket(buf.readBlockPos(), buf.readByte(), buf.readBoolean(), buf.readVarInt());
    }
}
