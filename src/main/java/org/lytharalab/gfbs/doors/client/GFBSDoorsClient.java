package org.lytharalab.gfbs.doors.client;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.lytharalab.gfbs.doors.content.ModBlockEntities;

@EventBusSubscriber(
    modid = "gfbs_misc",
    bus = Bus.MOD,
    value = {Dist.CLIENT}
)
public final class GFBSDoorsClient {
    private GFBSDoorsClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(RegisterRenderers event) {
        event.registerBlockEntityRenderer((BlockEntityType)ModBlockEntities.DOOR.get(), DoorRenderer::new);
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ClientReloadListener());
    }
}
