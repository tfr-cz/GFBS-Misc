package org.lytharalab.cabinelevator.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.lytharalab.gfbs.misc.network.MiscNetwork;

public final class ElevatorNetwork {
    private ElevatorNetwork() {
    }

    public static void sendController(ServerPlayer player, ControllerSnapshot snapshot) {
        sendController(player, snapshot, true);
    }

    public static void sendController(ServerPlayer player, ControllerSnapshot snapshot, boolean openIfMissing) {
        MiscNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenControllerPacket(snapshot, openIfMissing));
    }

    public static void sendInterface(ServerPlayer player, InterfaceSnapshot snapshot) {
        sendInterface(player, snapshot, true);
    }

    public static void sendInterface(ServerPlayer player, InterfaceSnapshot snapshot, boolean openIfMissing) {
        MiscNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenInterfacePacket(snapshot, openIfMissing));
    }

    public static void sendStatus(ServerPlayer player, String json) {
        MiscNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StatusPacket(json));
    }

    public static void sendStatus(ServerPlayer player, Component component) {
        sendStatus(player, Component.Serializer.toJson(component));
    }

    public static void saveController(SaveControllerPacket packet) {
        MiscNetwork.CHANNEL.sendToServer(packet);
    }

    public static void saveInterface(SaveInterfacePacket packet) {
        MiscNetwork.CHANNEL.sendToServer(packet);
    }

    public static void requestTravel(BlockPos controller, BlockPos target) {
        MiscNetwork.CHANNEL.sendToServer(new TravelRequestPacket(controller, target));
    }

    public static void requestDoorOpen(BlockPos controller) {
        MiscNetwork.CHANNEL.sendToServer(new OpenDoorPacket(controller));
    }
}
