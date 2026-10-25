package org.lytharalab.cabinelevator.network;

import net.minecraft.network.FriendlyByteBuf;

public record OpenInterfacePacket(InterfaceSnapshot snapshot, boolean openIfMissing) {
    public void encode(FriendlyByteBuf buf) {
        snapshot.encode(buf);
        buf.writeBoolean(openIfMissing);
    }

    public static OpenInterfacePacket decode(FriendlyByteBuf buf) {
        return new OpenInterfacePacket(InterfaceSnapshot.decode(buf), buf.readBoolean());
    }
}
