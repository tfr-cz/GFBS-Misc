package org.lytharalab.gfbs.doors.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.event.level.LevelEvent.Unload;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lytharalab.gfbs.doors.client.armor.HazmatVisorModel;
import org.lytharalab.gfbs.doors.client.armor.PowerArmorModels;
import net.minecraftforge.event.TickEvent;
import org.lytharalab.gfbs.doors.client.anim.CrucibleAnimations;
import org.lytharalab.gfbs.doors.client.anim.ItemAnimations;
import org.lytharalab.gfbs.doors.client.weapon.CrucibleModel;

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
        HazmatVisorModel.clear();        PowerArmorModels.clear();
        CrucibleModel.clear();
        ItemAnimations.clear();
    }

    /** Item animations are local-player state advanced by wall clock, so one tick is all they need. */
    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            CrucibleAnimations.tick();
        }
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
