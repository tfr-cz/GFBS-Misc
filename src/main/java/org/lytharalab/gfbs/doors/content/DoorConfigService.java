package org.lytharalab.gfbs.doors.content;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.lytharalab.gfbs.doors.content.block.DoorBlockEntity;
import org.lytharalab.gfbs.doors.network.DoorConfigSnapshot;
import org.lytharalab.gfbs.doors.network.DoorNetwork;
import org.lytharalab.gfbs.doors.network.DoorTogglePacket;
import org.lytharalab.gfbs.doors.network.SaveDoorConfigPacket;
import org.slf4j.Logger;

public final class DoorConfigService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double MAX_DISTANCE_SQ = 64.0D;

    private DoorConfigService() {
    }

    public static void open(ServerPlayer player, ServerLevel level, BlockPos pos) {
        DoorBlockEntity core = coreAt(level, pos);
        if (core == null) {
            return;
        }
        DoorNetwork.sendConfig(player, snapshot(core), true);
    }

    public static void save(ServerPlayer player, SaveDoorConfigPacket packet) {
        if (!canConfigure(player)) {
            reject(player);
            return;
        }
        if (!inReach(player, packet.pos())) {
            tooFar(player);
            return;
        }
        DoorBlockEntity core = coreAt(player.serverLevel(), packet.pos());
        if (core == null) {
            LOGGER.warn("Door config save from {} targeted a position without a door core: {}",
                    player.getGameProfile().getName(), packet.pos());
            return;
        }
        int mode = packet.mode();
        if (mode < 0 || mode >= DoorRedstoneMode.values().length) {
            return;
        }
        core.applyConfigToGroup(DoorRedstoneMode.values()[mode], packet.manual());
        DoorNetwork.sendConfig(player, snapshot(core), false);
    }

    public static void toggle(ServerPlayer player, DoorTogglePacket packet) {
        if (!canConfigure(player)) {
            reject(player);
            return;
        }
        if (!inReach(player, packet.pos())) {
            tooFar(player);
            return;
        }
        DoorBlockEntity core = coreAt(player.serverLevel(), packet.pos());
        if (core == null) {
            LOGGER.warn("Door toggle from {} targeted a position without a door core: {}",
                    player.getGameProfile().getName(), packet.pos());
            return;
        }
        core.commandToggle();
        DoorNetwork.sendConfig(player, snapshot(core), false);
    }

    private static boolean canConfigure(ServerPlayer player) {
        return player.isCreative() || player.hasPermissions(2);
    }

    private static boolean inReach(ServerPlayer player, BlockPos pos) {
        return player.distanceToSqr(Vec3.atCenterOf(pos)) <= MAX_DISTANCE_SQ;
    }

    private static void reject(ServerPlayer player) {
        player.displayClientMessage(
                Component.translatable("message.gfbs_doors.no_permission").withStyle(ChatFormatting.RED), true);
    }

    private static void tooFar(ServerPlayer player) {
        player.displayClientMessage(
                Component.translatable("message.gfbs_doors.too_far").withStyle(ChatFormatting.RED), true);
    }

    private static DoorBlockEntity coreAt(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof DoorBlockEntity part)) {
            return null;
        }
        return part.getCore();
    }

    private static DoorConfigSnapshot snapshot(DoorBlockEntity core) {
        return new DoorConfigSnapshot(core.getBlockPos(), core.getSpec().id,
                core.getRedstoneMode().ordinal(), core.isManualControl());
    }
}
