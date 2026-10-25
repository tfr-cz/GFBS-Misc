package org.lytharalab.cabinelevator.elevator;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lytharalab.gfbs.misc.GFBSMisc;

@Mod.EventBusSubscriber(modid = GFBSMisc.MODID)
public final class ElevatorEvents {
    private ElevatorEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ElevatorManager.onServerTick(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ElevatorManager.dispose(event.getServer());
    }
}
