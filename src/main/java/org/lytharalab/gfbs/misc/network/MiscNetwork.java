package org.lytharalab.gfbs.misc.network;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.lytharalab.cabinelevator.client.ClientPacketHandler;
import org.lytharalab.cabinelevator.elevator.ElevatorService;
import org.lytharalab.cabinelevator.network.OpenControllerPacket;
import org.lytharalab.cabinelevator.network.OpenInterfacePacket;
import org.lytharalab.cabinelevator.network.SaveControllerPacket;
import org.lytharalab.cabinelevator.network.SaveInterfacePacket;
import org.lytharalab.cabinelevator.network.StatusPacket;
import org.lytharalab.cabinelevator.network.TravelRequestPacket;
import org.lytharalab.gfbs.doors.client.DoorConfigScreen;
import org.lytharalab.gfbs.doors.content.DoorConfigService;
import org.lytharalab.gfbs.doors.network.CargoElevatorSyncPacket;
import org.lytharalab.gfbs.doors.network.DoorConfigPacket;
import org.lytharalab.gfbs.doors.network.DoorTogglePacket;
import org.lytharalab.gfbs.doors.network.SaveDoorConfigPacket;
import org.lytharalab.gfbs.misc.GFBSMisc;

import java.util.Optional;

/**
 * The single network channel owned by GFBS : Misc. Both the doors and the cabin
 * elevator register their packets here so the mod does not grow parallel
 * synchronization pipelines.
 */
public final class MiscNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GFBSMisc.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static int nextId;
    private static boolean registered;

    private MiscNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        CHANNEL.registerMessage(nextId++, OpenControllerPacket.class,
                OpenControllerPacket::encode, OpenControllerPacket::decode,
                (msg, ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                            () -> () -> ClientPacketHandler.openController(msg.snapshot(), msg.openIfMissing())));
                    ctx.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextId++, OpenInterfacePacket.class,
                OpenInterfacePacket::encode, OpenInterfacePacket::decode,
                (msg, ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                            () -> () -> ClientPacketHandler.openInterface(msg.snapshot(), msg.openIfMissing())));
                    ctx.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextId++, StatusPacket.class,
                StatusPacket::encode, StatusPacket::decode,
                (msg, ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                            () -> () -> ClientPacketHandler.showStatus(msg.json())));
                    ctx.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextId++, SaveControllerPacket.class,
                SaveControllerPacket::encode, SaveControllerPacket::decode,
                (msg, ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    ServerPlayer player = ctx.getSender();
                    if (player != null) {
                        ctx.enqueueWork(() -> ElevatorService.saveController(player, msg));
                    }
                    ctx.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(nextId++, SaveInterfacePacket.class,
                SaveInterfacePacket::encode, SaveInterfacePacket::decode,
                (msg, ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    ServerPlayer player = ctx.getSender();
                    if (player != null) {
                        ctx.enqueueWork(() -> ElevatorService.saveInterface(player, msg));
                    }
                    ctx.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(nextId++, TravelRequestPacket.class,
                TravelRequestPacket::encode, TravelRequestPacket::decode,
                (msg, ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    ServerPlayer player = ctx.getSender();
                    if (player != null) {
                        ctx.enqueueWork(() -> ElevatorService.requestTravel(player, msg));
                    }
                    ctx.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(nextId++, org.lytharalab.cabinelevator.network.OpenDoorPacket.class,
                org.lytharalab.cabinelevator.network.OpenDoorPacket::encode,
                org.lytharalab.cabinelevator.network.OpenDoorPacket::decode,
                (msg, ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    ServerPlayer player = ctx.getSender();
                    if (player != null) {
                        ctx.enqueueWork(() -> ElevatorService.requestDoorOpen(player, msg));
                    }
                    ctx.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(nextId++, DoorConfigPacket.class,
                DoorConfigPacket::encode, DoorConfigPacket::decode,
                (msg, ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                            () -> () -> DoorConfigScreen.open(msg.snapshot(), msg.openIfMissing())));
                    ctx.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextId++, SaveDoorConfigPacket.class,
                SaveDoorConfigPacket::encode, SaveDoorConfigPacket::decode,
                (msg, ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    ServerPlayer player = ctx.getSender();
                    if (player != null) {
                        ctx.enqueueWork(() -> DoorConfigService.save(player, msg));
                    }
                    ctx.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(nextId++, DoorTogglePacket.class,
                DoorTogglePacket::encode, DoorTogglePacket::decode,
                (msg, ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    ServerPlayer player = ctx.getSender();
                    if (player != null) {
                        ctx.enqueueWork(() -> DoorConfigService.toggle(player, msg));
                    }
                    ctx.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(nextId++, CargoElevatorSyncPacket.class,
                CargoElevatorSyncPacket::encode, CargoElevatorSyncPacket::decode,
                (msg, ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                            () -> () -> org.lytharalab.gfbs.doors.client.CargoElevatorClientSync
                                    .apply(msg.pos(), msg.extension())));
                    ctx.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendCargoSync(ServerLevel level, BlockPos pos, double extension) {
        CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
                new CargoElevatorSyncPacket(pos, extension));
    }
}
