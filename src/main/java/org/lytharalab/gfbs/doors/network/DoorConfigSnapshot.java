package org.lytharalab.gfbs.doors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record DoorConfigSnapshot(BlockPos pos, String doorId, int mode, boolean manual, int extraHeight, boolean elevator) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeUtf(this.doorId, 64);
        buf.writeByte(this.mode);
        buf.writeBoolean(this.manual);
        buf.writeVarInt(this.extraHeight);
        buf.writeBoolean(this.elevator);
    }

    public static DoorConfigSnapshot decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String doorId = buf.readUtf(64);
        int mode = buf.readByte();
        boolean manual = buf.readBoolean();
        int extraHeight = buf.readVarInt();
        boolean elevator = buf.readBoolean();
        return new DoorConfigSnapshot(pos, doorId, mode, manual, extraHeight, elevator);
    }
}
