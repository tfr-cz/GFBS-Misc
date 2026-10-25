package org.lytharalab.gfbs.doors.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lytharalab.gfbs.misc.GFBSMisc;

@Mod.EventBusSubscriber(modid = GFBSMisc.MODID, value = Dist.CLIENT)
public final class GFBSDoorsClientEvents {
    private GFBSDoorsClientEvents() {
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        DoorClientSoundHooks.clearAll();
        DoorVisualCache.clear();
        CargoElevatorVisualCache.clear();
        DoorModelAssets.clear();
        DoorItemRenderer.clear();
        TransitionSealAnimation.clear();
    }

    @SubscribeEvent
    public static void levelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            DoorClientSoundHooks.clearAll();
        }
    }
}
