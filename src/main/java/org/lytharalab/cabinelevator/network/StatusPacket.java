package org.lytharalab.cabinelevator.network;

import net.minecraft.network.FriendlyByteBuf;

public record StatusPacket(String json) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(json, 32767);
    }

    public static StatusPacket decode(FriendlyByteBuf buf) {
        return new StatusPacket(buf.readUtf(32767));
    }
}
