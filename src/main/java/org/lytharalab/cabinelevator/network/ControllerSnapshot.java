package org.lytharalab.cabinelevator.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;
import org.lytharalab.cabinelevator.data.RangeBox;

import java.util.ArrayList;
import java.util.List;

public final class ControllerSnapshot {
    public static final int MAX_LIST_SIZE = 4096;

    public final BlockPos pos;
    public final boolean configMode;
    public final String channel;
    public final String floorName;
    public final RangeBox range;
    public final int timeTicks;
    public final List<ChannelInfo> channels;
    public final List<FloorInfo> floors;
    @Nullable
    public final BlockPos currentFloor;
    public final boolean idle;

    public ControllerSnapshot(BlockPos pos, boolean configMode, String channel, String floorName, RangeBox range,
                              int timeTicks, List<ChannelInfo> channels, List<FloorInfo> floors,
                              @Nullable BlockPos currentFloor, boolean idle) {
        this.pos = pos;
        this.configMode = configMode;
        this.channel = channel;
        this.floorName = floorName;
        this.range = range;
        this.timeTicks = timeTicks;
        this.channels = List.copyOf(channels);
        this.floors = List.copyOf(floors);
        this.currentFloor = currentFloor;
        this.idle = idle;
    }

    public static final class ChannelInfo {
        public final String name;
        public final RangeBox range;
        public final int timeTicks;
        public final int floorsHere;
        public final int floorsTotal;

        public ChannelInfo(String name, RangeBox range, int timeTicks, int floorsHere, int floorsTotal) {
            this.name = name;
            this.range = range;
            this.timeTicks = timeTicks;
            this.floorsHere = floorsHere;
            this.floorsTotal = floorsTotal;
        }
    }

    public static final class FloorInfo {
        public final BlockPos pos;
        public final String name;
        public final int index;

        public FloorInfo(BlockPos pos, String name, int index) {
            this.pos = pos;
            this.name = name;
            this.index = index;
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(configMode);
        buf.writeUtf(channel, 64);
        buf.writeUtf(floorName, 64);
        writeRange(buf, range);
        buf.writeVarInt(timeTicks);
        buf.writeVarInt(channels.size());
        for (ChannelInfo info : channels) {
            buf.writeUtf(info.name, 64);
            writeRange(buf, info.range);
            buf.writeVarInt(info.timeTicks);
            buf.writeVarInt(info.floorsHere);
            buf.writeVarInt(info.floorsTotal);
        }
        buf.writeVarInt(floors.size());
        for (FloorInfo floor : floors) {
            buf.writeBlockPos(floor.pos);
            buf.writeUtf(floor.name, 64);
            buf.writeVarInt(floor.index);
        }
        buf.writeBoolean(currentFloor != null);
        if (currentFloor != null) {
            buf.writeBlockPos(currentFloor);
        }
        buf.writeBoolean(idle);
    }

    public static ControllerSnapshot decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        boolean configMode = buf.readBoolean();
        String channel = buf.readUtf(64);
        String floorName = buf.readUtf(64);
        RangeBox range = readRange(buf);
        int time = buf.readVarInt();
        int channelCount = Math.min(buf.readVarInt(), MAX_LIST_SIZE);
        List<ChannelInfo> channels = new ArrayList<>(channelCount);
        for (int i = 0; i < channelCount; i++) {
            channels.add(new ChannelInfo(buf.readUtf(64), readRange(buf), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt()));
        }
        int floorCount = Math.min(buf.readVarInt(), MAX_LIST_SIZE);
        List<FloorInfo> floors = new ArrayList<>(floorCount);
        for (int i = 0; i < floorCount; i++) {
            floors.add(new FloorInfo(buf.readBlockPos(), buf.readUtf(64), buf.readVarInt()));
        }
        BlockPos current = buf.readBoolean() ? buf.readBlockPos() : null;
        boolean idle = buf.readBoolean();
        return new ControllerSnapshot(pos, configMode, channel, floorName, range, time, channels, floors, current, idle);
    }

    public static void writeRange(FriendlyByteBuf buf, RangeBox range) {
        buf.writeInt(range.minX());
        buf.writeInt(range.minY());
        buf.writeInt(range.minZ());
        buf.writeInt(range.maxX());
        buf.writeInt(range.maxY());
        buf.writeInt(range.maxZ());
    }

    public static RangeBox readRange(FriendlyByteBuf buf) {
        return new RangeBox(buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readInt(), buf.readInt(), buf.readInt());
    }
}
