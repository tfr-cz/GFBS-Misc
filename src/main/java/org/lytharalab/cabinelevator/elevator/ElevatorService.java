package org.lytharalab.cabinelevator.elevator;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.lytharalab.cabinelevator.content.block.ElevatorCallerBlock;
import org.lytharalab.cabinelevator.content.block.ElevatorControllerBlock;
import org.lytharalab.cabinelevator.content.block.RedstoneInterfaceBlock;
import org.lytharalab.cabinelevator.data.DimPos;
import org.lytharalab.cabinelevator.data.ElevatorSavedData;
import org.lytharalab.cabinelevator.network.ControllerSnapshot;
import org.lytharalab.cabinelevator.network.ElevatorNetwork;
import org.lytharalab.cabinelevator.network.InterfaceSnapshot;
import org.lytharalab.cabinelevator.network.OpenDoorPacket;
import org.lytharalab.cabinelevator.network.SaveControllerPacket;
import org.lytharalab.cabinelevator.network.SaveInterfacePacket;
import org.lytharalab.cabinelevator.network.TravelRequestPacket;
import org.lytharalab.cabinelevator.util.ElevatorUtil;
import org.lytharalab.cabinelevator.util.Msg;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ElevatorService {
    private static final double MAX_INTERACTION_DISTANCE_SQ = 64.0D;

    private ElevatorService() {
    }

    public static void openController(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof ElevatorControllerBlock)) {
            Msg.error(player, "message.cabin_elevator.not_controller");
            return;
        }
        ElevatorSavedData data = ElevatorSavedData.get(level.getServer());
        data.getOrCreateController(DimPos.of(level, pos));
        ElevatorNetwork.sendController(player, buildControllerSnapshot(player, level, pos, data));
    }

    public static void openInterface(ServerPlayer player, ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof RedstoneInterfaceBlock)) {
            Msg.error(player, "message.cabin_elevator.not_interface");
            return;
        }
        ElevatorSavedData data = ElevatorSavedData.get(level.getServer());
        data.getOrCreateInterface(DimPos.of(level, pos), RedstoneInterfaceBlock.maskFromState(state));
        ElevatorNetwork.sendInterface(player, buildInterfaceSnapshot(player, level, pos, data));
    }

    private static ControllerSnapshot buildControllerSnapshot(ServerPlayer player, ServerLevel level, BlockPos pos,
                                                              ElevatorSavedData data) {
        ElevatorSavedData.ControllerEntry entry = data.getOrCreateController(DimPos.of(level, pos));
        org.lytharalab.cabinelevator.data.RangeBox range = data.effectiveRange(entry);
        int timeTicks = data.effectiveTime(entry);
        List<ControllerSnapshot.FloorInfo> floors = new ArrayList<>();
        if (!entry.channel.isBlank()) {
            List<DimPos> positions = data.floors(level.dimension(), entry.channel);
            for (int i = 0; i < positions.size(); i++) {
                DimPos floor = positions.get(i);
                ElevatorSavedData.ControllerEntry floorEntry = data.controller(floor);
                floors.add(new ControllerSnapshot.FloorInfo(floor.pos(),
                        floorEntry == null ? "" : floorEntry.floorName, i));
            }
        }
        List<ControllerSnapshot.ChannelInfo> channels = new ArrayList<>();
        for (String name : data.channelNames()) {
            ElevatorSavedData.ChannelSetting setting = data.channel(name);
            if (setting == null) {
                continue;
            }
            channels.add(new ControllerSnapshot.ChannelInfo(name, setting.range, setting.timeTicks,
                    data.floors(level.dimension(), name).size(), data.floorCount(name)));
        }
        DimPos current = entry.channel.isBlank() ? null : data.currentFloor(level.dimension(), entry.channel);
        boolean idle = ElevatorManager.get(level.getServer()).state(level.dimension(), entry.channel)
                == ElevatorManager.State.IDLE;
        return new ControllerSnapshot(pos, player.isCreative(), entry.channel, entry.floorName, range,
                timeTicks, channels, floors, current == null ? null : current.pos(), idle);
    }

    private static InterfaceSnapshot buildInterfaceSnapshot(ServerPlayer player, ServerLevel level, BlockPos pos,
                                                            ElevatorSavedData data) {
        BlockState state = level.getBlockState(pos);
        int stateFaces = state.getBlock() instanceof RedstoneInterfaceBlock
                ? RedstoneInterfaceBlock.maskFromState(state) : 1 << Direction.NORTH.get3DDataValue();
        ElevatorSavedData.InterfaceEntry entry = data.getOrCreateInterface(DimPos.of(level, pos), stateFaces);
        boolean bound = entry.controller != null;
        boolean controllerValid = false;
        String channel = "";
        String floorName = "";
        BlockPos controllerPos = entry.controller == null ? null : entry.controller.pos();
        List<InterfaceSnapshot.BoundInterface> boundInterfaces = new ArrayList<>();

        if (entry.controller != null) {
            ElevatorSavedData.ControllerEntry controllerEntry = data.controller(entry.controller);
            ServerLevel controllerLevel = entry.controller.level(level.getServer());
            controllerValid = controllerEntry != null && controllerLevel != null
                    && ElevatorUtil.isController(controllerLevel, entry.controller.pos());
            if (controllerEntry != null) {
                channel = controllerEntry.channel;
                floorName = controllerEntry.floorName;
            }
            for (DimPos interfacePos : data.interfacesBoundTo(entry.controller)) {
                ElevatorSavedData.InterfaceEntry other = data.interfaceAt(interfacePos);
                if (other == null) {
                    continue;
                }
                ServerLevel otherLevel = interfacePos.level(level.getServer());
                boolean valid = otherLevel != null && ElevatorUtil.isInterface(otherLevel, interfacePos.pos());
                boundInterfaces.add(new InterfaceSnapshot.BoundInterface(interfacePos.pos(), other.priority, valid));
            }
        }

        return new InterfaceSnapshot(pos, bound, controllerValid, channel, floorName, controllerPos,
                entry.priority, entry.facesMask, entry.strength, entry.durationTicks, entry.nextDelayTicks,
                entry.arrivalDelayTicks, boundInterfaces);
    }

    public static void saveController(ServerPlayer player, SaveControllerPacket packet) {
        if (!player.isCreative()) {
            ElevatorNetwork.sendStatus(player, Msg.serializedError("message.cabin_elevator.not_creative"));
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = packet.pos();
        if (!isWithinReach(player, pos)) {
            ElevatorNetwork.sendStatus(player, Msg.serializedError("message.cabin_elevator.too_far"));
            return;
        }
        if (!(level.getBlockState(pos).getBlock() instanceof ElevatorControllerBlock)) {
            ElevatorNetwork.sendStatus(player, Msg.serializedError("message.cabin_elevator.controller_missing"));
            return;
        }
        String channel = ElevatorUtil.sanitizeName(packet.channel(), 32);
        if (channel.isBlank()) {
            ElevatorNetwork.sendStatus(player, Msg.serializedError("message.cabin_elevator.config.channel_required"));
            return;
        }
        String floorName = ElevatorUtil.sanitizeName(packet.floorName(), 32);
        int timeTicks = packet.timeTicks();
        if (timeTicks < 0 || timeTicks > ElevatorUtil.MAX_TIME_TICKS) {
            ElevatorNetwork.sendStatus(player, Msg.serializedError("message.cabin_elevator.config.invalid_time",
                    0, ElevatorUtil.MAX_TIME_TICKS / 20));
            return;
        }

        ElevatorSavedData data = ElevatorSavedData.get(level.getServer());
        DimPos key = DimPos.of(level, pos);
        ElevatorSavedData.ControllerEntry entry = data.getOrCreateController(key);
        String oldChannel = entry.channel;
        data.setControllerConfig(key, channel, floorName, timeTicks);
        ElevatorManager.get(level.getServer()).onControllerConfigured(key, oldChannel, channel);
        ElevatorNetwork.sendController(player, buildControllerSnapshot(player, level, pos, data), false);
        ElevatorNetwork.sendStatus(player, Msg.serializedSuccess("message.cabin_elevator.config.saved"));
    }

    public static void saveInterface(ServerPlayer player, SaveInterfacePacket packet) {
        if (!player.isCreative()) {
            ElevatorNetwork.sendStatus(player, Msg.serializedError("message.cabin_elevator.not_creative"));
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = packet.pos();
        if (!isWithinReach(player, pos)) {
            ElevatorNetwork.sendStatus(player, Msg.serializedError("message.cabin_elevator.too_far"));
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof RedstoneInterfaceBlock)) {
            ElevatorNetwork.sendStatus(player, Msg.serializedError("message.cabin_elevator.not_interface"));
            return;
        }
        int priority = packet.priority();
        if (priority < ElevatorUtil.MIN_PRIORITY || priority > ElevatorUtil.MAX_PRIORITY) {
            ElevatorNetwork.sendStatus(player, Msg.serializedError("message.cabin_elevator.interface.invalid_priority"));
            return;
        }
        int strength = packet.strength();
        if (strength < 0 || strength > 15) {
            ElevatorNetwork.sendStatus(player, Msg.serializedError("message.cabin_elevator.interface.invalid_strength"));
            return;
        }
        int duration = packet.durationTicks();
        int nextDelay = packet.nextDelayTicks();
        int arrivalDelay = packet.arrivalDelayTicks();
        if (duration < 0 || duration > ElevatorUtil.MAX_TIME_TICKS
                || nextDelay < 0 || nextDelay > ElevatorUtil.MAX_TIME_TICKS
                || arrivalDelay < 0 || arrivalDelay > ElevatorUtil.MAX_TIME_TICKS) {
            ElevatorNetwork.sendStatus(player, Msg.serializedError("message.cabin_elevator.interface.invalid_time",
                    0, ElevatorUtil.MAX_TIME_TICKS / 20));
            return;
        }
        int facesMask = packet.facesMask();
        if (facesMask < 1 || facesMask > 63) {
            ElevatorNetwork.sendStatus(player, Msg.serializedError("message.cabin_elevator.interface.no_faces"));
            return;
        }

        ElevatorSavedData data = ElevatorSavedData.get(level.getServer());
        DimPos key = DimPos.of(level, pos);
        ElevatorSavedData.InterfaceEntry entry = data.getOrCreateInterface(key,
                RedstoneInterfaceBlock.maskFromState(state));
        entry.priority = priority;
        entry.facesMask = facesMask;
        entry.strength = strength;
        entry.durationTicks = duration;
        entry.nextDelayTicks = nextDelay;
        entry.arrivalDelayTicks = arrivalDelay;
        data.setDirty();
        if (level.isLoaded(pos)) {
            BlockState withFaces = RedstoneInterfaceBlock.applyFaces(state, facesMask);
            if (withFaces != state) {
                level.setBlock(pos, withFaces, 3);
                RedstoneInterfaceBlock.notifyOutputs(level, pos, withFaces);
            }
        }
        ElevatorNetwork.sendInterface(player, buildInterfaceSnapshot(player, level, pos, data), false);
        ElevatorNetwork.sendStatus(player, Msg.serializedSuccess("message.cabin_elevator.interface.saved"));
    }

    public static boolean bindInterface(ServerPlayer player, ServerLevel level, BlockPos pos, DimPos controller) {
        if (!isWithinReach(player, pos)) {
            Msg.error(player, "message.cabin_elevator.too_far");
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof RedstoneInterfaceBlock)) {
            Msg.error(player, "message.cabin_elevator.not_interface");
            return false;
        }
        if (!controller.dimension().equals(level.dimension())) {
            Msg.error(player, "message.cabin_elevator.dim_mismatch");
            return false;
        }
        ElevatorSavedData data = ElevatorSavedData.get(level.getServer());
        ServerLevel controllerLevel = controller.level(level.getServer());
        if (data.controller(controller) == null || controllerLevel == null
                || !ElevatorUtil.isController(controllerLevel, controller.pos())) {
            Msg.error(player, "message.cabin_elevator.controller_missing");
            return false;
        }
        DimPos key = DimPos.of(level, pos);
        ElevatorSavedData.InterfaceEntry entry = data.getOrCreateInterface(key,
                RedstoneInterfaceBlock.maskFromState(state));
        if (!controller.equals(entry.controller)) {
            entry.controller = controller;
            data.setDirty();
        }
        Msg.success(player, "message.cabin_elevator.linker.interface_bound", entry.priority,
                controller.pos().getX(), controller.pos().getY(), controller.pos().getZ());
        return true;
    }

    public static boolean bindCaller(ServerPlayer player, ServerLevel level, BlockPos pos, DimPos controller) {
        if (!isWithinReach(player, pos)) {
            Msg.error(player, "message.cabin_elevator.too_far");
            return false;
        }
        if (!(level.getBlockState(pos).getBlock() instanceof ElevatorCallerBlock)) {
            Msg.error(player, "message.cabin_elevator.not_caller");
            return false;
        }
        if (!controller.dimension().equals(level.dimension())) {
            Msg.error(player, "message.cabin_elevator.dim_mismatch");
            return false;
        }
        ElevatorSavedData data = ElevatorSavedData.get(level.getServer());
        ServerLevel controllerLevel = controller.level(level.getServer());
        if (data.controller(controller) == null || controllerLevel == null
                || !ElevatorUtil.isController(controllerLevel, controller.pos())) {
            Msg.error(player, "message.cabin_elevator.controller_missing");
            return false;
        }
        DimPos key = DimPos.of(level, pos);
        DimPos existing = data.callerFor(controller);
        if (existing != null && !existing.equals(key)) {
            Msg.error(player, "message.cabin_elevator.caller_taken");
            return false;
        }
        ElevatorSavedData.CallerEntry entry = data.getOrCreateCaller(key);
        entry.controller = controller;
        data.setDirty();
        Msg.success(player, "message.cabin_elevator.linker.caller_bound",
                controller.pos().getX(), controller.pos().getY(), controller.pos().getZ());
        return true;
    }

    public static void showCallerStatus(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof ElevatorCallerBlock)) {
            Msg.error(player, "message.cabin_elevator.not_caller");
            return;
        }
        ElevatorSavedData data = ElevatorSavedData.get(level.getServer());
        ElevatorSavedData.CallerEntry caller = data.callerAt(DimPos.of(level, pos));
        DimPos controller = caller == null ? null : caller.controller;
        if (controller == null) {
            Msg.error(player, "message.cabin_elevator.caller_unbound");
            return;
        }
        ServerLevel controllerLevel = controller.level(level.getServer());
        if (data.controller(controller) == null || controllerLevel == null
                || !ElevatorUtil.isController(controllerLevel, controller.pos())) {
            Msg.error(player, "message.cabin_elevator.controller_missing");
            return;
        }
        ElevatorSavedData.ControllerEntry entry = data.controller(controller);
        Component bound = Component.translatable("message.cabin_elevator.caller.status.bound",
                controller.pos().getX(), controller.pos().getY(), controller.pos().getZ());
        player.sendSystemMessage(bound.copy().withStyle(ChatFormatting.YELLOW));
        if (entry == null || entry.channel.isBlank()) {
            Msg.info(player, "message.cabin_elevator.caller.status.no_channel");
            return;
        }
        DimPos current = data.currentFloor(controller.dimension(), entry.channel);
        int index = current == null ? -1 : data.floorIndex(controller.dimension(), entry.channel, current.pos());
        Component floor = current == null || index < 0
                ? Component.translatable("gui.cabin_elevator.none")
                : ElevatorUtil.floorComponent(data, current, index);
        ElevatorManager.State state = ElevatorManager.get(level.getServer())
                .state(controller.dimension(), entry.channel);
        Msg.info(player, "message.cabin_elevator.caller.status.details",
                Component.literal(entry.channel), floor, Component.translatable(stateKey(state)));
    }

    public static void requestTravel(ServerPlayer player, TravelRequestPacket packet) {
        ServerLevel level = player.serverLevel();
        boolean started = ElevatorManager.get(level.getServer()).requestRide(player,
                DimPos.of(level, packet.controller()), DimPos.of(level, packet.target()));
        if (started) {
            ElevatorSavedData data = ElevatorSavedData.get(level.getServer());
            ElevatorNetwork.sendController(player,
                    buildControllerSnapshot(player, level, packet.controller(), data), false);
        }
    }

    public static void requestDoorOpen(ServerPlayer player, OpenDoorPacket packet) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = packet.controller();
        if (!isWithinReach(player, pos)) {
            ElevatorNetwork.sendStatus(player, Msg.serializedError("message.cabin_elevator.too_far"));
            return;
        }
        if (!(level.getBlockState(pos).getBlock() instanceof ElevatorControllerBlock)) {
            ElevatorNetwork.sendStatus(player, Msg.serializedError("message.cabin_elevator.controller_missing"));
            return;
        }
        boolean started = ElevatorManager.get(level.getServer()).requestDoorOpen(player, DimPos.of(level, pos));
        if (started) {
            ElevatorSavedData data = ElevatorSavedData.get(level.getServer());
            ElevatorNetwork.sendController(player, buildControllerSnapshot(player, level, pos, data), false);
        }
    }

    public static boolean applyRange(ServerPlayer player, ServerLevel level,
                                     @Nullable DimPos storedController, BlockPos a, BlockPos b) {
        if (!player.isCreative()) {
            Msg.error(player, "message.cabin_elevator.range.creative_only");
            return false;
        }
        int minX = Math.min(a.getX(), b.getX());
        int minY = Math.min(a.getY(), b.getY());
        int minZ = Math.min(a.getZ(), b.getZ());
        int maxX = Math.max(a.getX(), b.getX()) + 1;
        int maxY = Math.max(a.getY(), b.getY()) + 1;
        int maxZ = Math.max(a.getZ(), b.getZ()) + 1;
        if (maxX - minX > org.lytharalab.cabinelevator.data.RangeBox.MAX_SIZE
                || maxY - minY > org.lytharalab.cabinelevator.data.RangeBox.MAX_SIZE
                || maxZ - minZ > org.lytharalab.cabinelevator.data.RangeBox.MAX_SIZE) {
            Msg.error(player, "message.cabin_elevator.range.too_large",
                    org.lytharalab.cabinelevator.data.RangeBox.MAX_SIZE);
            return false;
        }

        ElevatorSavedData data = ElevatorSavedData.get(level.getServer());
        DimPos controller = storedController;
        if (controller == null) {
            List<DimPos> found = new ArrayList<>();
            for (Map.Entry<DimPos, ElevatorSavedData.ControllerEntry> candidate : data.controllers().entrySet()) {
                DimPos key = candidate.getKey();
                if (!key.dimension().equals(level.dimension())) {
                    continue;
                }
                BlockPos pos = key.pos();
                if (pos.getX() >= minX && pos.getX() < maxX && pos.getY() >= minY && pos.getY() < maxY
                        && pos.getZ() >= minZ && pos.getZ() < maxZ) {
                    ServerLevel candidateLevel = key.level(level.getServer());
                    if (candidateLevel != null && ElevatorUtil.isController(candidateLevel, pos)) {
                        found.add(key);
                    }
                }
            }
            if (found.isEmpty()) {
                Msg.error(player, "message.cabin_elevator.range.no_controller");
                return false;
            }
            if (found.size() > 1) {
                Msg.error(player, "message.cabin_elevator.range.multiple_controllers");
                return false;
            }
            controller = found.get(0);
        }

        if (!controller.dimension().equals(level.dimension())) {
            Msg.error(player, "message.cabin_elevator.dim_mismatch");
            return false;
        }
        ElevatorSavedData.ControllerEntry entry = data.controller(controller);
        ServerLevel controllerLevel = controller.level(level.getServer());
        if (entry == null || controllerLevel == null
                || !ElevatorUtil.isController(controllerLevel, controller.pos())) {
            Msg.error(player, "message.cabin_elevator.controller_missing");
            return false;
        }
        if (entry.channel.isBlank()) {
            Msg.error(player, "message.cabin_elevator.range.no_channel");
            return false;
        }

        org.lytharalab.cabinelevator.data.RangeBox range = new org.lytharalab.cabinelevator.data.RangeBox(
                minX - controller.pos().getX(), minY - controller.pos().getY(), minZ - controller.pos().getZ(),
                maxX - controller.pos().getX(), maxY - controller.pos().getY(), maxZ - controller.pos().getZ());
        if (!range.isValid()) {
            Msg.error(player, "message.cabin_elevator.range.too_large",
                    org.lytharalab.cabinelevator.data.RangeBox.MAX_SIZE);
            return false;
        }
        data.setChannelRange(entry.channel, range);
        Msg.success(player, "message.cabin_elevator.range.applied",
                range.width(), range.height(), range.depth(),
                range.minX(), range.minY(), range.minZ(), range.maxX(), range.maxY(), range.maxZ());
        return true;
    }

    private static boolean isWithinReach(ServerPlayer player, BlockPos pos) {
        return player.distanceToSqr(Vec3.atCenterOf(pos)) <= MAX_INTERACTION_DISTANCE_SQ;
    }

    private static String stateKey(ElevatorManager.State state) {
        return switch (state) {
            case IDLE -> "gui.cabin_elevator.state.idle";
            case TRAVELING -> "gui.cabin_elevator.state.traveling";
            case SEQUENCING -> "gui.cabin_elevator.state.sequencing";
        };
    }
}
