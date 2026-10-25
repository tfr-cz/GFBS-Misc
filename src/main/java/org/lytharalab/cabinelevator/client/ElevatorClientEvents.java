package org.lytharalab.cabinelevator.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lytharalab.cabinelevator.content.ModBlockEntities;
import org.lytharalab.gfbs.misc.GFBSMisc;

@Mod.EventBusSubscriber(modid = GFBSMisc.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ElevatorClientEvents {
    private ElevatorClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.CONTROLLER.get(), RangeOverlayRenderer::new);
    }
}
