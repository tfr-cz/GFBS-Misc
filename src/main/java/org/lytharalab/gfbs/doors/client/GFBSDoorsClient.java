package org.lytharalab.gfbs.doors.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lytharalab.gfbs.misc.GFBSMisc;
import org.lytharalab.gfbs.doors.content.ModBlockEntities;

@Mod.EventBusSubscriber(modid = GFBSMisc.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GFBSDoorsClient {
    private GFBSDoorsClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.DOOR.get(), DoorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CARGO_ELEVATOR.get(), CargoElevatorRenderer::new);
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ClientReloadListener());
    }
}
