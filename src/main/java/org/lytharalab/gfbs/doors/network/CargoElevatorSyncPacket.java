package org.lytharalab.gfbs.doors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/** Lightweight per-tick extension update for the cargo elevator (chunk tracked). */
public record CargoElevatorSyncPacket(BlockPos pos, double extension) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeDouble(extension);
    }

    public static CargoElevatorSyncPacket decode(FriendlyByteBuf buf) {
        return new CargoElevatorSyncPacket(buf.readBlockPos(), buf.readDouble());
    }
}
