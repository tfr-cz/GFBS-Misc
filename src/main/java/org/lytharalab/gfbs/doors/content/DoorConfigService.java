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
    private static final double MAX_DISTANCE_SQ = 64.0;

    private DoorConfigService() {
    }

    public static void open(ServerPlayer player, ServerLevel level, BlockPos pos) {
        DoorBlockEntity door = doorCoreAt(level, pos);
        if (door != null) {
            DoorNetwork.sendConfig(player, snapshot(door, pos), true);
        }
    }

    public static void save(ServerPlayer player, SaveDoorConfigPacket packet) {
        if (!canConfigure(player)) {
            reject(player);
        } else if (!inReach(player, packet.pos())) {
            tooFar(player);
        } else {
            int mode = packet.mode();
            if (mode >= 0 && mode < DoorRedstoneMode.values().length) {
                DoorRedstoneMode requested = DoorRedstoneMode.values()[mode];
                DoorBlockEntity door = doorCoreAt(player.serverLevel(), packet.pos());
                if (door != null) {
                    int extraHeight = "blast_door".equals(door.getSpec().id) ? packet.extraHeight() : 0;
                    boolean heightApplied = door.applyConfigToGroup(requested, packet.manual(), extraHeight);
                    if (!heightApplied) {
                        player.displayClientMessage(Component.translatable("message.gfbs_doors.blast_height_blocked").withStyle(ChatFormatting.RED), true);
                    }

                    DoorNetwork.sendConfig(player, snapshot(door, packet.pos()), false);
                } else {
                    LOGGER.warn("Door config save from {} targeted a position without a door core: {}", player.getGameProfile().getName(), packet.pos());
                }
            }
        }
    }

    public static void toggle(ServerPlayer player, DoorTogglePacket packet) {
        if (!canConfigure(player)) {
            reject(player);
        } else if (!inReach(player, packet.pos())) {
            tooFar(player);
        } else {
            DoorBlockEntity door = doorCoreAt(player.serverLevel(), packet.pos());
            if (door != null) {
                door.commandToggle();
                DoorNetwork.sendConfig(player, snapshot(door, packet.pos()), false);
            } else {
                LOGGER.warn("Door toggle from {} targeted a position without a door core: {}", player.getGameProfile().getName(), packet.pos());
            }
        }
    }

    private static boolean canConfigure(ServerPlayer player) {
        return player.isCreative() || player.hasPermissions(2);
    }

    private static boolean inReach(ServerPlayer player, BlockPos pos) {
        return player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64.0;
    }

    private static void reject(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.gfbs_doors.no_permission").withStyle(ChatFormatting.RED), true);
    }

    private static void tooFar(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.gfbs_doors.too_far").withStyle(ChatFormatting.RED), true);
    }

    private static DoorBlockEntity doorCoreAt(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof DoorBlockEntity part ? part.getCore() : null;
    }

    private static DoorConfigSnapshot snapshot(DoorBlockEntity core, BlockPos at) {
        return new DoorConfigSnapshot(at, core.getSpec().id, core.getRedstoneMode().ordinal(), core.isManualControl(), core.getExtraHeight(), false);
    }
}
