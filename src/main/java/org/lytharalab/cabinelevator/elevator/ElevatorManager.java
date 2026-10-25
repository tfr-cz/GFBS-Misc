package org.lytharalab.cabinelevator.elevator;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.lytharalab.cabinelevator.content.block.RedstoneInterfaceBlock;
import org.lytharalab.cabinelevator.data.DimPos;
import org.lytharalab.cabinelevator.data.ElevatorSavedData;
import org.lytharalab.cabinelevator.data.RangeBox;
import org.lytharalab.cabinelevator.util.ElevatorUtil;
import org.lytharalab.cabinelevator.util.Msg;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

public final class ElevatorManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile ElevatorManager active;
    private static final int MAX_TASKS_PER_TICK = 2000;
    private static final int MAX_CHUNK_WAIT_TICKS = 600;
    private static final int HUM_INTERVAL_TICKS = 30;

    public enum State {
        IDLE,
        TRAVELING,
        SEQUENCING
    }

    private record FloorKey(ResourceKey<Level> dimension, String channel) {
    }

    private record Task(long at, long order, Runnable action) {
    }

    private static final class Travel {
        final FloorKey key;
        final DimPos source;
        final DimPos destination;
        final boolean teleportPlayers;
        final UUID initiator;
        final long endTick;
        boolean active = true;

        Travel(FloorKey key, DimPos source, DimPos destination, boolean teleportPlayers, UUID initiator,
               long endTick) {
            this.key = key;
            this.source = source;
            this.destination = destination;
            this.teleportPlayers = teleportPlayers;
            this.initiator = initiator;
            this.endTick = endTick;
        }
    }

    private static final class Sequence {
        final FloorKey key;
        final DimPos destination;
        final UUID initiator;
        final List<DimPos> floors;
        final boolean updateCurrentFloor;
        final Set<DimPos> fired = new HashSet<>();
        int pending;
        boolean active = true;

        Sequence(FloorKey key, DimPos destination, UUID initiator, List<DimPos> floors, boolean updateCurrentFloor) {
            this.key = key;
            this.destination = destination;
            this.initiator = initiator;
            this.floors = floors;
            this.updateCurrentFloor = updateCurrentFloor;
        }
    }

    private static final class ElevatorRuntime {
        State state = State.IDLE;
        Travel travel;
        Sequence sequence;
    }

    private final MinecraftServer server;
    private final ElevatorSavedData data;
    private final PriorityQueue<Task> tasks = new PriorityQueue<>((a, b) -> {
        int byTime = Long.compare(a.at, b.at);
        return byTime != 0 ? byTime : Long.compare(a.order, b.order);
    });
    private final Map<FloorKey, ElevatorRuntime> runtimes = new HashMap<>();
    private long nextOrder;

    private ElevatorManager(MinecraftServer server) {
        this.server = server;
        this.data = ElevatorSavedData.get(server);
    }

    public static ElevatorManager get(MinecraftServer server) {
        ElevatorManager manager = active;
        if (manager != null && manager.server == server) {
            return manager;
        }
        synchronized (ElevatorManager.class) {
            if (active == null || active.server != server) {
                active = new ElevatorManager(server);
            }
            return active;
        }
    }

    public static void onServerTick(MinecraftServer server) {
        ElevatorManager manager = active;
        if (manager != null && manager.server == server) {
            manager.tick();
        }
    }

    public static void dispose(MinecraftServer server) {
        synchronized (ElevatorManager.class) {
            if (active != null && active.server == server) {
                active.tasks.clear();
                active.runtimes.clear();
                active = null;
            }
        }
    }

    public State state(ResourceKey<Level> dimension, String channel) {
        if (channel == null || channel.isBlank()) {
            return State.IDLE;
        }
        ElevatorRuntime runtime = runtimes.get(new FloorKey(dimension, channel));
        return runtime == null ? State.IDLE : runtime.state;
    }

    public boolean requestRide(ServerPlayer player, DimPos source, DimPos target) {
        ServerLevel level = player.serverLevel();
        if (!source.dimension().equals(level.dimension())) {
            Msg.error(player, "message.cabin_elevator.dim_mismatch");
            return false;
        }
        if (!source.dimension().equals(target.dimension())) {
            Msg.error(player, "message.cabin_elevator.dim_mismatch");
            return false;
        }
        if (!ElevatorUtil.isController(level, source.pos()) || !ElevatorUtil.isController(level, target.pos())) {
            Msg.error(player, "message.cabin_elevator.controller_missing");
            return false;
        }
        ElevatorSavedData.ControllerEntry sourceEntry = data.controller(source);
        ElevatorSavedData.ControllerEntry targetEntry = data.controller(target);
        if (sourceEntry == null || targetEntry == null || sourceEntry.channel.isBlank()
                || !sourceEntry.channel.equals(targetEntry.channel)) {
            Msg.error(player, "message.cabin_elevator.not_configured");
            return false;
        }
        List<DimPos> floors = data.floors(source.dimension(), sourceEntry.channel);
        int sourceIndex = floors.indexOf(source);
        int targetIndex = floors.indexOf(target);
        if (sourceIndex < 0 || targetIndex < 0) {
            Msg.error(player, "message.cabin_elevator.not_configured");
            return false;
        }
        if (sourceIndex == targetIndex) {
            Msg.error(player, "message.cabin_elevator.same_floor");
            return false;
        }
        RangeBox range = data.effectiveRange(sourceEntry);
        if (!ElevatorUtil.isPlayerInRange(level, source, range, player)) {
            Msg.error(player, "message.cabin_elevator.out_of_range");
            return false;
        }
        FloorKey key = new FloorKey(source.dimension(), sourceEntry.channel);
        if (state(source.dimension(), sourceEntry.channel) != State.IDLE) {
            Msg.error(player, "message.cabin_elevator.busy");
            return false;
        }
        int ticks = data.effectiveTime(sourceEntry) * Math.abs(targetIndex - sourceIndex);
        startTravel(key, source, target, true, player, ticks);
        Msg.action(player, "message.cabin_elevator.ride.started",
                ElevatorUtil.floorComponent(data, target, targetIndex), ElevatorUtil.formatSeconds(ticks));
        return true;
    }

    public boolean requestCall(@Nullable ServerPlayer player, DimPos caller) {
        ServerLevel level = player != null ? player.serverLevel() : server.getLevel(caller.dimension());
        if (level == null || !caller.dimension().equals(level.dimension()) || !ElevatorUtil.isCaller(level, caller.pos())) {
            if (player != null) {
                Msg.error(player, "message.cabin_elevator.caller_missing");
            }
            return false;
        }
        ElevatorSavedData.CallerEntry callerEntry = data.callerAt(caller);
        DimPos destination = callerEntry == null ? null : callerEntry.controller;
        if (destination == null) {
            if (player != null) {
                Msg.error(player, "message.cabin_elevator.caller_unbound");
            }
            return false;
        }
        if (!destination.dimension().equals(level.dimension())
                || !ElevatorUtil.isController(level, destination.pos())) {
            if (player != null) {
                Msg.error(player, "message.cabin_elevator.controller_missing");
            }
            return false;
        }
        ElevatorSavedData.ControllerEntry destinationEntry = data.controller(destination);
        if (destinationEntry == null || destinationEntry.channel.isBlank()) {
            if (player != null) {
                Msg.error(player, "message.cabin_elevator.not_configured");
            }
            return false;
        }
        DimPos source = data.currentFloor(destination.dimension(), destinationEntry.channel);
        if (source == null) {
            if (player != null) {
                Msg.error(player, "message.cabin_elevator.no_floors");
            }
            return false;
        }
        if (!ElevatorUtil.isController(level, source.pos())) {
            source = destination;
        }
        List<DimPos> floors = data.floors(destination.dimension(), destinationEntry.channel);
        int sourceIndex = floors.indexOf(source);
        int destinationIndex = floors.indexOf(destination);
        if (sourceIndex < 0 || destinationIndex < 0) {
            if (player != null) {
                Msg.error(player, "message.cabin_elevator.no_floors");
            }
            return false;
        }
        FloorKey key = new FloorKey(destination.dimension(), destinationEntry.channel);
        if (state(destination.dimension(), destinationEntry.channel) != State.IDLE) {
            if (player != null) {
                Msg.error(player, "message.cabin_elevator.busy");
            }
            return false;
        }
        int ticks = data.effectiveTime(destinationEntry) * Math.abs(destinationIndex - sourceIndex);
        startTravel(key, source, destination, false, player, ticks);
        if (player != null) {
            Msg.action(player, "message.cabin_elevator.call.started",
                    ElevatorUtil.floorComponent(data, destination, destinationIndex),
                    ElevatorUtil.formatSeconds(ticks));
        }
        return true;
    }

    public boolean requestDoorOpen(@Nullable ServerPlayer player, DimPos controller) {
        ServerLevel level = player != null ? player.serverLevel() : server.getLevel(controller.dimension());
        if (level == null || !controller.dimension().equals(level.dimension())
                || !ElevatorUtil.isController(level, controller.pos())) {
            if (player != null) {
                Msg.error(player, "message.cabin_elevator.controller_missing");
            }
            return false;
        }
        ElevatorSavedData.ControllerEntry entry = data.controller(controller);
        if (entry == null || entry.channel.isBlank()) {
            if (player != null) {
                Msg.error(player, "message.cabin_elevator.not_configured");
            }
            return false;
        }
        FloorKey key = new FloorKey(controller.dimension(), entry.channel);
        if (state(key.dimension(), key.channel()) != State.IDLE) {
            if (player != null) {
                Msg.error(player, "message.cabin_elevator.busy");
            }
            return false;
        }
        List<DimPos> interfaces = data.interfacesBoundTo(controller);
        if (interfaces.isEmpty()) {
            if (player != null) {
                Msg.error(player, "message.cabin_elevator.door.no_interfaces");
            }
            return false;
        }
        ElevatorRuntime runtime = runtimes.computeIfAbsent(key, ignored -> new ElevatorRuntime());
        runtime.state = State.SEQUENCING;
        runtime.travel = null;
        beginSequence(runtime, key, controller, player == null ? null : player.getUUID(),
                interfaces, false, false);
        if (player != null) {
            int index = data.floorIndex(controller.dimension(), entry.channel, controller.pos());
            Msg.action(player, "message.cabin_elevator.door.opened",
                    ElevatorUtil.floorComponent(data, controller, Math.max(0, index)));
        }
        return true;
    }

    private void startTravel(FloorKey key, DimPos source, DimPos destination, boolean teleportPlayers,
                             ServerPlayer initiator, int durationTicks) {
        ServerLevel level = server.getLevel(source.dimension());
        if (level == null) {
            return;
        }
        ElevatorRuntime runtime = runtimes.computeIfAbsent(key, ignored -> new ElevatorRuntime());
        long endTick = server.getTickCount() + Math.max(0, durationTicks);
        Travel travel = new Travel(key, source, destination, teleportPlayers,
                initiator == null ? null : initiator.getUUID(), endTick);
        runtime.state = State.TRAVELING;
        runtime.travel = travel;
        runtime.sequence = null;

        playAt(level, source.pos(), SoundEvents.PISTON_EXTEND, 0.65F, 0.75F);
        playAt(level, destination.pos(), SoundEvents.PISTON_EXTEND, 0.55F, 0.8F);
        playAt(level, source.pos(), SoundEvents.MINECART_INSIDE, 0.45F, 0.9F);
        playAt(level, destination.pos(), SoundEvents.MINECART_INSIDE, 0.35F, 0.9F);
        if (durationTicks > HUM_INTERVAL_TICKS) {
            scheduleHum(travel);
        }
        schedule(Math.max(0, durationTicks), () -> finishTravel(runtime, travel));
    }

    private void scheduleHum(Travel travel) {
        if (!travel.active) {
            return;
        }
        if (server.getTickCount() + HUM_INTERVAL_TICKS >= travel.endTick) {
            return;
        }
        schedule(HUM_INTERVAL_TICKS, () -> {
            if (!travel.active) {
                return;
            }
            ServerLevel level = server.getLevel(travel.source.dimension());
            if (level != null) {
                playAt(level, travel.source.pos(), SoundEvents.MINECART_INSIDE, 0.45F, 0.9F);
                playAt(level, travel.destination.pos(), SoundEvents.MINECART_INSIDE, 0.35F, 0.9F);
            }
            scheduleHum(travel);
        });
    }

    private void finishTravel(ElevatorRuntime runtime, Travel travel) {
        if (runtime.state != State.TRAVELING || runtime.travel != travel || !travel.active) {
            return;
        }
        travel.active = false;
        runtime.travel = null;

        ServerLevel level = server.getLevel(travel.source.dimension());
        if (level == null || !ElevatorUtil.isController(level, travel.source.pos())
                || !ElevatorUtil.isController(level, travel.destination.pos())) {
            cancel(runtime, travel.initiator, "message.cabin_elevator.run_cancelled");
            return;
        }
        ElevatorSavedData.ControllerEntry sourceEntry = data.controller(travel.source);
        ElevatorSavedData.ControllerEntry destinationEntry = data.controller(travel.destination);
        if (sourceEntry == null || destinationEntry == null || destinationEntry.channel.isBlank()
                || !destinationEntry.channel.equals(travel.key.channel())) {
            cancel(runtime, travel.initiator, "message.cabin_elevator.run_cancelled");
            return;
        }

        if (travel.teleportPlayers) {
            RangeBox range = data.effectiveRange(sourceEntry);
            List<ServerPlayer> players = ElevatorUtil.playersInRange(level, travel.source.pos(), range);
            for (ServerPlayer player : players) {
                Vec3 target = ElevatorUtil.relativeDestination(travel.source.pos(), travel.destination.pos(),
                        player.position());
                player.teleportTo(level, target.x, target.y, target.z, player.getYRot(), player.getXRot());
                player.setDeltaMovement(Vec3.ZERO);
                player.resetFallDistance();
            }
            if (!players.isEmpty()) {
                playAt(level, travel.destination.pos(), SoundEvents.ENDERMAN_TELEPORT, 0.6F, 1.05F);
            }
        }
        playAt(level, travel.destination.pos(), SoundEvents.NOTE_BLOCK_BELL.value(), 0.75F, 1.35F);

        runtime.state = State.SEQUENCING;
        startSequence(runtime, travel);
    }

    private void startSequence(ElevatorRuntime runtime, Travel travel) {
        beginSequence(runtime, travel.key, travel.destination, travel.initiator,
                data.interfacesBoundTo(travel.destination), true, true);
    }

    private void beginSequence(ElevatorRuntime runtime, FloorKey key, DimPos destination, UUID initiator,
                               List<DimPos> interfaces, boolean announceArrival, boolean updateCurrentFloor) {
        Sequence sequence = new Sequence(key, destination, initiator, new ArrayList<>(interfaces),
                updateCurrentFloor);
        runtime.sequence = sequence;
        runtime.state = State.SEQUENCING;

        if (announceArrival && initiator != null) {
            int index = data.floorIndex(destination.dimension(), key.channel(), destination.pos());
            ServerPlayer initiatorPlayer = server.getPlayerList().getPlayer(initiator);
            if (initiatorPlayer != null && index >= 0) {
                Msg.action(initiatorPlayer, "message.cabin_elevator.arrived",
                        ElevatorUtil.floorComponent(data, destination, index));
            }
        }

        if (sequence.floors.isEmpty()) {
            standby(runtime, sequence);
            return;
        }
        int delay = 0;
        if (priorityOf(sequence, 0) == 1) {
            for (DimPos interfacePos : sequence.floors) {
                ElevatorSavedData.InterfaceEntry entry = data.interfaceAt(interfacePos);
                if (entry == null || entry.priority != 1) {
                    continue;
                }
                delay = Math.max(delay, Math.max(0, entry.arrivalDelayTicks));
            }
        }
        scheduleActivation(runtime, sequence, 0, delay);
        maybeFinish(runtime, sequence);
    }

    private void scheduleActivation(ElevatorRuntime runtime, Sequence sequence, int index, int delay) {
        sequence.pending++;
        schedule(Math.max(0, delay), () -> {
            if (!isActive(runtime, sequence)) {
                return;
            }
            sequence.pending--;
            activate(runtime, sequence, index, 0);
        });
    }

    private void activate(ElevatorRuntime runtime, Sequence sequence, int startIndex, int waitedTicks) {
        if (!isActive(runtime, sequence)) {
            return;
        }
        if (startIndex >= sequence.floors.size()) {
            maybeFinish(runtime, sequence);
            return;
        }
        int stageEnd = stageEnd(sequence, startIndex);
        boolean retry = false;

        for (int i = startIndex; i < stageEnd; i++) {
            DimPos interfacePos = sequence.floors.get(i);
            if (sequence.fired.contains(interfacePos)) {
                continue;
            }
            ServerLevel level = server.getLevel(interfacePos.dimension());
            if (level == null) {
                sequence.fired.add(interfacePos);
                continue;
            }
            if (!level.isLoaded(interfacePos.pos())) {
                if (waitedTicks < MAX_CHUNK_WAIT_TICKS) {
                    retry = true;
                } else {
                    LOGGER.warn("Skipping redstone interface at {} in {} because its chunk stayed unloaded",
                            interfacePos.pos(), interfacePos.dimension().location());
                    sequence.fired.add(interfacePos);
                }
                continue;
            }
            ElevatorSavedData.InterfaceEntry entry = data.interfaceAt(interfacePos);
            BlockState state = level.getBlockState(interfacePos.pos());
            sequence.fired.add(interfacePos);
            if (entry == null || !sequence.destination.equals(entry.controller)
                    || !(state.getBlock() instanceof RedstoneInterfaceBlock)) {
                continue;
            }
            fireSignal(level, interfacePos.pos(), state, entry);
            sequence.pending++;
            schedule(Math.max(0, entry.durationTicks), () -> {
                if (!isActive(runtime, sequence)) {
                    return;
                }
                sequence.pending--;
                maybeFinish(runtime, sequence);
            });
        }

        if (retry) {
            sequence.pending++;
            schedule(20, () -> {
                if (!isActive(runtime, sequence)) {
                    return;
                }
                sequence.pending--;
                activate(runtime, sequence, startIndex, waitedTicks + 20);
            });
            maybeFinish(runtime, sequence);
            return;
        }

        if (stageEnd < sequence.floors.size()) {
            scheduleActivation(runtime, sequence, stageEnd, stageDelay(sequence, startIndex, stageEnd));
        }
        maybeFinish(runtime, sequence);
    }

    private int stageEnd(Sequence sequence, int startIndex) {
        int priority = priorityOf(sequence, startIndex);
        int end = startIndex + 1;
        while (end < sequence.floors.size() && priorityOf(sequence, end) == priority) {
            end++;
        }
        return end;
    }

    private int priorityOf(Sequence sequence, int index) {
        ElevatorSavedData.InterfaceEntry entry = data.interfaceAt(sequence.floors.get(index));
        return entry == null ? Integer.MAX_VALUE : entry.priority;
    }

    private int stageDelay(Sequence sequence, int start, int end) {
        int delay = 0;
        for (int i = start; i < end; i++) {
            ElevatorSavedData.InterfaceEntry entry = data.interfaceAt(sequence.floors.get(i));
            if (entry != null) {
                delay = Math.max(delay, Math.max(0, entry.nextDelayTicks));
            }
        }
        return delay;
    }

    private void fireSignal(ServerLevel level, BlockPos pos, BlockState state,
                            ElevatorSavedData.InterfaceEntry entry) {
        int strength = Math.max(0, Math.min(15, entry.strength));
        if (strength <= 0 || entry.durationTicks <= 0) {
            return;
        }
        BlockState powered = RedstoneInterfaceBlock.applyFaces(state, entry.facesMask)
                .setValue(RedstoneInterfaceBlock.POWER, strength);
        level.setBlock(pos, powered, 3);
        RedstoneInterfaceBlock.notifyOutputs(level, pos, powered);
        level.scheduleTick(pos, powered.getBlock(), Math.max(1, entry.durationTicks));
    }

    private void maybeFinish(ElevatorRuntime runtime, Sequence sequence) {
        if (isActive(runtime, sequence) && sequence.pending <= 0) {
            standby(runtime, sequence);
        }
    }

    private void standby(ElevatorRuntime runtime, Sequence sequence) {
        if (runtime.sequence != sequence) {
            return;
        }
        sequence.active = false;
        runtime.sequence = null;
        runtime.state = State.IDLE;
        runtime.travel = null;
        runtimes.remove(sequence.key, runtime);
        if (sequence.updateCurrentFloor) {
            data.setCurrentFloor(sequence.key.dimension(), sequence.key.channel(), sequence.destination.pos());
        }
    }

    private boolean isActive(ElevatorRuntime runtime, Sequence sequence) {
        return runtime.state == State.SEQUENCING && runtime.sequence == sequence && sequence.active;
    }

    private void cancel(ElevatorRuntime runtime, UUID initiator, String messageKey) {
        runtime.state = State.IDLE;
        if (runtime.travel != null) {
            runtime.travel.active = false;
        }
        if (runtime.sequence != null) {
            runtime.sequence.active = false;
        }
        runtime.travel = null;
        runtime.sequence = null;
        if (initiator != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(initiator);
            if (player != null) {
                Msg.error(player, messageKey);
            }
        }
    }

    public void onControllerRemoved(DimPos controller) {
        Iterator<Map.Entry<FloorKey, ElevatorRuntime>> iterator = runtimes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<FloorKey, ElevatorRuntime> mapEntry = iterator.next();
            if (!mapEntry.getKey().dimension().equals(controller.dimension())) {
                continue;
            }
            ElevatorRuntime runtime = mapEntry.getValue();
            boolean affected = runtime.travel != null
                    && (controller.equals(runtime.travel.source) || controller.equals(runtime.travel.destination));
            affected = affected || runtime.sequence != null && controller.equals(runtime.sequence.destination);
            if (affected) {
                UUID initiator = runtime.travel != null ? runtime.travel.initiator
                        : runtime.sequence != null ? runtime.sequence.initiator : null;
                cancel(runtime, initiator, "message.cabin_elevator.run_cancelled");
                iterator.remove();
            }
        }
    }

    public void onControllerConfigured(DimPos controller, String oldChannel, String newChannel) {
        if (oldChannel.equals(newChannel)) {
            return;
        }
        Iterator<Map.Entry<FloorKey, ElevatorRuntime>> iterator = runtimes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<FloorKey, ElevatorRuntime> mapEntry = iterator.next();
            if (!mapEntry.getKey().dimension().equals(controller.dimension())) {
                continue;
            }
            ElevatorRuntime runtime = mapEntry.getValue();
            boolean affected = runtime.travel != null
                    && (controller.equals(runtime.travel.source) || controller.equals(runtime.travel.destination));
            affected = affected || runtime.sequence != null && controller.equals(runtime.sequence.destination);
            if (affected) {
                UUID initiator = runtime.travel != null ? runtime.travel.initiator
                        : runtime.sequence != null ? runtime.sequence.initiator : null;
                cancel(runtime, initiator, "message.cabin_elevator.run_cancelled");
                iterator.remove();
            }
        }
    }

    private void schedule(int delayTicks, Runnable action) {
        long at = server.getTickCount() + Math.max(0, delayTicks);
        tasks.add(new Task(at, nextOrder++, action));
    }

    private void tick() {
        if (tasks.isEmpty()) {
            return;
        }
        long now = server.getTickCount();
        int processed = 0;
        while (!tasks.isEmpty() && tasks.peek().at <= now && processed < MAX_TASKS_PER_TICK) {
            Task task = tasks.poll();
            try {
                task.action.run();
            } catch (Throwable throwable) {
                LOGGER.error("Elevator task failed", throwable);
            }
            processed++;
        }
    }

    private void playAt(ServerLevel level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        if (level.isLoaded(pos)) {
            level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    sound, SoundSource.BLOCKS, volume, pitch);
        }
    }
}
