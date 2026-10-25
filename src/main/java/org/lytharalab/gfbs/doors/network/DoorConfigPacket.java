package org.lytharalab.gfbs.doors.network;

import net.minecraft.network.FriendlyByteBuf;

public record DoorConfigPacket(DoorConfigSnapshot snapshot, boolean openIfMissing) {
    public void encode(FriendlyByteBuf buf) {
        snapshot.encode(buf);
        buf.writeBoolean(openIfMissing);
    }

    public static DoorConfigPacket decode(FriendlyByteBuf buf) {
        return new DoorConfigPacket(DoorConfigSnapshot.decode(buf), buf.readBoolean());
    }
}
