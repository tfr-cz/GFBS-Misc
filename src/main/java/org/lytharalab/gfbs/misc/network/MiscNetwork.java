package org.lytharalab.gfbs.misc.network;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.simple.SimpleChannel;
import org.lytharalab.gfbs.doors.client.DoorConfigScreen;
import org.lytharalab.gfbs.doors.content.DoorConfigService;
import org.lytharalab.gfbs.doors.network.DoorConfigPacket;
import org.lytharalab.gfbs.doors.network.DoorTogglePacket;
import org.lytharalab.gfbs.doors.network.SaveDoorConfigPacket;

public final class MiscNetwork {
    private static final String PROTOCOL = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("gfbs_misc", "main"), () -> "2", "2"::equals, "2"::equals);
    private static int nextId;
    private static boolean registered;

    private MiscNetwork() {
    }

    public static void register() {
        if (!registered) {
            registered = true;
            CHANNEL.registerMessage(nextId++, DoorConfigPacket.class, DoorConfigPacket::encode, DoorConfigPacket::decode, (msg, ctxSupplier) -> {
                Context ctx = (Context)ctxSupplier.get();
                ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DoorConfigScreen.open(msg.snapshot(), msg.openIfMissing())));
                ctx.setPacketHandled(true);
            }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
            CHANNEL.registerMessage(nextId++, SaveDoorConfigPacket.class, SaveDoorConfigPacket::encode, SaveDoorConfigPacket::decode, (msg, ctxSupplier) -> {
                Context ctx = (Context)ctxSupplier.get();
                ServerPlayer player = ctx.getSender();
                if (player != null) {
                    ctx.enqueueWork(() -> DoorConfigService.save(player, msg));
                }

                ctx.setPacketHandled(true);
            }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
            CHANNEL.registerMessage(nextId++, DoorTogglePacket.class, DoorTogglePacket::encode, DoorTogglePacket::decode, (msg, ctxSupplier) -> {
                Context ctx = (Context)ctxSupplier.get();
                ServerPlayer player = ctx.getSender();
                if (player != null) {
                    ctx.enqueueWork(() -> DoorConfigService.toggle(player, msg));
                }

                ctx.setPacketHandled(true);
            }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        }
    }
}
