package org.lytharalab.cabinelevator.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record SaveInterfacePacket(BlockPos pos, int priority, int facesMask, int strength, int durationTicks,
                                  int nextDelayTicks, int arrivalDelayTicks) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(priority);
        buf.writeVarInt(facesMask);
        buf.writeVarInt(strength);
        buf.writeVarInt(durationTicks);
        buf.writeVarInt(nextDelayTicks);
        buf.writeVarInt(arrivalDelayTicks);
    }

    public static SaveInterfacePacket decode(FriendlyByteBuf buf) {
        return new SaveInterfacePacket(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }
}
