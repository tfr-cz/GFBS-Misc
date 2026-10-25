package org.lytharalab.gfbs.doors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.lytharalab.gfbs.misc.network.MiscNetwork;

public final class DoorNetwork {
    private DoorNetwork() {
    }

    public static void sendConfig(ServerPlayer player, DoorConfigSnapshot snapshot, boolean openIfMissing) {
        MiscNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new DoorConfigPacket(snapshot, openIfMissing));
    }

    public static void save(BlockPos pos, int mode, boolean manual) {
        MiscNetwork.CHANNEL.sendToServer(new SaveDoorConfigPacket(pos, mode, manual));
    }

    public static void toggle(BlockPos pos) {
        MiscNetwork.CHANNEL.sendToServer(new DoorTogglePacket(pos));
    }
}
