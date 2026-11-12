package org.lytharalab.gfbs.doors.compat;

import com.mojang.logging.LogUtils;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lytharalab.gfbs.doors.content.DoorCoreRegistry;
import org.lytharalab.gfbs.doors.content.DoorRedstoneMode;
import org.lytharalab.gfbs.doors.content.block.DoorBlockEntity;
import org.lytharalab.gfbs.main.content.acomplex.gate.GateDefinition;
import org.lytharalab.gfbs.main.content.acomplex.gate.GateDefinitions;
import org.lytharalab.gfbs.main.content.acomplex.gate.GateSavedData;
import org.slf4j.Logger;

@EventBusSubscriber(
    modid = "gfbs_misc"
)
public final class GfbsMainGateFollow {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int POLL_INTERVAL_TICKS = 10;
    private static int tickCounter;
    private static boolean reportedFailure;

    private GfbsMainGateFollow() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        if (event.phase == Phase.END && ++tickCounter >= 10) {
            tickCounter = 0;
            if (ModList.get().isLoaded("gfbs_main")) {
                try {
                    applyDesiredStates();
                } catch (Throwable var2) {
                    if (!reportedFailure) {
                        reportedFailure = true;
                        LOGGER.warn("Unable to follow GFBS : Main gate states; GFBS door modes will not react to /GFBsMain gate commands", var2);
                    }
                }
            }
        }
    }

    private static void applyDesiredStates() {
        GateDefinition closedGate = (GateDefinition)GateDefinitions.find("gate").orElse(null);
        GateDefinition openGate = (GateDefinition)GateDefinitions.find("check_point_gate").orElse(null);
        if (closedGate != null && openGate != null) {
            for (DoorBlockEntity core : DoorCoreRegistry.cores()) {
                if (!core.isRemoved() && core.isCore()) {
                    DoorRedstoneMode mode = core.getRedstoneMode();
                    if (mode.isGfbs()) {
                        Level normallyOpen = core.getLevel();
                        if (normallyOpen instanceof ServerLevel) {
                            ServerLevel level = (ServerLevel)normallyOpen;
                            boolean normallyOpenx = mode == DoorRedstoneMode.GFBS_NORMALLY_OPEN;
                            Optional data = savedData(level);
                            if (!data.isEmpty()) {
                                boolean desired = ((GateSavedData)data.get()).desired(normallyOpenx ? openGate : closedGate, normallyOpenx);
                                core.applyGateDesiredState(desired);
                            }
                        }
                    }
                }
            }
        }
    }

    private static Optional savedData(ServerLevel level) {
        try {
            return Optional.ofNullable(GateSavedData.get(level));
        } catch (Throwable var2) {
            return Optional.empty();
        }
    }
}
