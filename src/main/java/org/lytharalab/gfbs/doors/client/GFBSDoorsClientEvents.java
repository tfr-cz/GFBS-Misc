package org.lytharalab.gfbs.doors.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.event.level.LevelEvent.Unload;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
    modid = "gfbs_misc",
    value = {Dist.CLIENT}
)
public final class GFBSDoorsClientEvents {
    private GFBSDoorsClientEvents() {
    }

    @SubscribeEvent
    public static void logout(LoggingOut event) {
        DoorClientSoundHooks.clearAll();
        DoorVisualCache.clear();
        DoorModelAssets.clear();
        DoorItemRenderer.clear();
        TransitionSealAnimation.clear();
    }

    @SubscribeEvent
    public static void levelUnload(Unload event) {
        if (event.getLevel().isClientSide()) {
            DoorClientSoundHooks.clearAll();
            DoorVisualCache.clear();
        }
    }

    @SubscribeEvent
    public static void chunkUnload(net.minecraftforge.event.level.ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            DoorClientSoundHooks.onChunkUnload(event.getChunk().getPos());
        }
    }
}
