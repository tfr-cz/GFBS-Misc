package org.lytharalab.cabinelevator.network;

import net.minecraft.network.FriendlyByteBuf;

public record OpenControllerPacket(ControllerSnapshot snapshot, boolean openIfMissing) {
    public void encode(FriendlyByteBuf buf) {
        snapshot.encode(buf);
        buf.writeBoolean(openIfMissing);
    }

    public static OpenControllerPacket decode(FriendlyByteBuf buf) {
        return new OpenControllerPacket(ControllerSnapshot.decode(buf), buf.readBoolean());
    }
}
