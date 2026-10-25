package org.lytharalab.gfbs.doors.content;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.lytharalab.gfbs.doors.content.block.CargoElevatorBlockEntity;
import org.lytharalab.gfbs.misc.GFBSMisc;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Carries riders at the end of the tick, after entity physics and before rendering.
 * Doing this inside the block entity tick would let gravity pull riders down again
 * before the frame is drawn, and a rider caught inside the freshly raised collision
 * slab would fall straight through it.
 */
@Mod.EventBusSubscriber(modid = GFBSMisc.MODID)
public final class CargoElevatorRiders {
    private static final CopyOnWriteArrayList<CargoElevatorBlockEntity> ACTIVE = new CopyOnWriteArrayList<>();


    private CargoElevatorRiders() {
    }

    public static void register(CargoElevatorBlockEntity elevator) {
        if (!ACTIVE.contains(elevator)) {
            ACTIVE.add(elevator);
        }
    }

    public static void unregister(CargoElevatorBlockEntity elevator) {
        ACTIVE.remove(elevator);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (CargoElevatorBlockEntity elevator : ACTIVE) {
            elevator.carryRiders();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            for (CargoElevatorBlockEntity elevator : ACTIVE) {
                org.lytharalab.gfbs.doors.client.CargoElevatorRiderHooks.carryLocalPlayer(elevator);
            }
        });
    }
}
