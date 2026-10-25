package org.lytharalab.gfbs.doors.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.lytharalab.gfbs.doors.content.DoorCoreRegistry;
import org.lytharalab.gfbs.doors.content.DoorRedstoneMode;
import org.lytharalab.gfbs.doors.content.block.DoorBlockEntity;
import org.lytharalab.gfbs.main.content.acomplex.gate.GateDefinition;
import org.lytharalab.gfbs.main.content.acomplex.gate.GateDefinitions;
import org.lytharalab.gfbs.main.content.acomplex.gate.GateSavedData;
import org.lytharalab.gfbs.misc.GFBSMisc;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Makes GFBS : Misc normally-closed doors behave like GFBS : Main's {@code gate} blast
 * doors and normally-open doors like its {@code check_point_gate} doors: both follow the
 * host mod's persisted, command-driven desired state, so /GFBsMain gate commands apply.
 * Reads only public API (GateDefinitions/GateSavedData); safe to keep optional.
 */
@Mod.EventBusSubscriber(modid = GFBSMisc.MODID)
public final class GfbsMainGateFollow {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int POLL_INTERVAL_TICKS = 10;
    private static int tickCounter;
    private static boolean reportedFailure;

    private GfbsMainGateFollow() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++tickCounter < POLL_INTERVAL_TICKS) return;
        tickCounter = 0;
        if (!ModList.get().isLoaded("gfbs_main")) return;
        try {
            applyDesiredStates();
        } catch (Throwable throwable) {
            if (!reportedFailure) {
                reportedFailure = true;
                LOGGER.warn("Unable to follow GFBS : Main gate states; GFBS door modes will not react to /GFBsMain gate commands", throwable);
            }
        }
    }

    private static void applyDesiredStates() {
        GateDefinition closedGate = GateDefinitions.find("gate").orElse(null);
        GateDefinition openGate = GateDefinitions.find("check_point_gate").orElse(null);
        if (closedGate == null || openGate == null) return;

        for (DoorBlockEntity core : DoorCoreRegistry.cores()) {
            if (core.isRemoved() || !core.isCore()) continue;
            DoorRedstoneMode mode = core.getRedstoneMode();
            if (!mode.isGfbs() || !(core.getLevel() instanceof ServerLevel level)) continue;
            boolean normallyOpen = mode == DoorRedstoneMode.GFBS_NORMALLY_OPEN;
            Optional<GateSavedData> data = savedData(level);
            if (data.isEmpty()) continue;
            boolean desired = data.get().desired(normallyOpen ? openGate : closedGate, normallyOpen);
            core.applyGateDesiredState(desired);
        }
    }

    private static Optional<GateSavedData> savedData(ServerLevel level) {
        try {
            return Optional.ofNullable(GateSavedData.get(level));
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }
}
