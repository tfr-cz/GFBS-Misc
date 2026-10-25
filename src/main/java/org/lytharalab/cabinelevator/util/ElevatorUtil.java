package org.lytharalab.cabinelevator.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lytharalab.cabinelevator.content.block.ElevatorCallerBlock;
import org.lytharalab.cabinelevator.content.block.ElevatorControllerBlock;
import org.lytharalab.cabinelevator.content.block.RedstoneInterfaceBlock;
import org.lytharalab.cabinelevator.data.DimPos;
import org.lytharalab.cabinelevator.data.ElevatorSavedData;
import org.lytharalab.cabinelevator.data.RangeBox;

import java.util.List;
import java.util.Locale;

public final class ElevatorUtil {
    public static final int MIN_RANGE = 1;
    public static final int MAX_RANGE = 64;
    public static final int MAX_TIME_TICKS = 6000;
    public static final int MIN_PRIORITY = 1;
    public static final int MAX_PRIORITY = 999;

    private ElevatorUtil() {
    }

    public static AABB rangeAabb(BlockPos controller, RangeBox range) {
        return range.aabb(controller);
    }

    public static boolean isPlayerInRange(ServerLevel level, DimPos controller, RangeBox range,
                                          ServerPlayer player) {
        if (!level.dimension().equals(controller.dimension())) {
            return false;
        }
        return player.getBoundingBox().intersects(rangeAabb(controller.pos(), range));
    }

    public static List<ServerPlayer> playersInRange(ServerLevel level, BlockPos controller, RangeBox range) {
        return level.getEntitiesOfClass(ServerPlayer.class, rangeAabb(controller, range).inflate(0.1D),
                player -> !player.isSpectator());
    }

    public static Vec3 relativeDestination(BlockPos source, BlockPos destination, Vec3 playerPos) {
        Vec3 sourceCenter = Vec3.atCenterOf(source);
        Vec3 destinationCenter = Vec3.atCenterOf(destination);
        return destinationCenter.add(playerPos.subtract(sourceCenter));
    }

    public static String sanitizeName(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.strip();
        StringBuilder builder = new StringBuilder(Math.min(trimmed.length(), maxLength));
        for (int i = 0; i < trimmed.length() && builder.length() < maxLength; i++) {
            char c = trimmed.charAt(i);
            if (c >= 0x20 && c != 0x7F && c != '\u00a7') {
                builder.append(c);
            }
        }
        return builder.toString().strip();
    }

    public static int parsePriority(String value) {
        try {
            int parsed = Integer.parseInt(value.strip());
            if (parsed < MIN_PRIORITY || parsed > MAX_PRIORITY) {
                return -1;
            }
            return parsed;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static double parseSeconds(String value) {
        try {
            double parsed = Double.parseDouble(value.strip());
            return Double.isFinite(parsed) && parsed >= 0.0D ? parsed : -1.0D;
        } catch (NumberFormatException e) {
            return -1.0D;
        }
    }

    public static int secondsToTicks(double seconds) {
        return (int) Math.round(Mth.clamp(seconds, 0.0D, MAX_TIME_TICKS / 20.0D) * 20.0D);
    }

    public static String formatSeconds(int ticks) {
        return String.format(Locale.ROOT, "%.1f", ticks / 20.0D);
    }

    public static Component floorComponent(ElevatorSavedData data, DimPos floor, int index) {
        ElevatorSavedData.ControllerEntry entry = data.controller(floor);
        if (entry != null && !entry.floorName.isBlank()) {
            return Component.literal(entry.floorName);
        }
        return Component.translatable("gui.cabin_elevator.floor_fallback", index + 1);
    }

    public static Component channelComponent(String channel) {
        return Component.literal(channel);
    }

    public static Component directionName(Direction direction) {
        return Component.translatable("direction.cabin_elevator." + direction.getSerializedName());
    }

    public static boolean isController(ServerLevel level, BlockPos pos) {
        if (level == null || !level.isLoaded(pos)) {
            return true;
        }
        return level.getBlockState(pos).getBlock() instanceof ElevatorControllerBlock;
    }

    public static boolean isInterface(ServerLevel level, BlockPos pos) {
        if (level == null || !level.isLoaded(pos)) {
            return true;
        }
        return level.getBlockState(pos).getBlock() instanceof RedstoneInterfaceBlock;
    }

    public static boolean isCaller(ServerLevel level, BlockPos pos) {
        if (level == null || !level.isLoaded(pos)) {
            return true;
        }
        return level.getBlockState(pos).getBlock() instanceof ElevatorCallerBlock;
    }
}
