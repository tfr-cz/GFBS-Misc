package org.lytharalab.cabinelevator.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class InterfaceSnapshot {
    public static final int MAX_LIST_SIZE = 1024;

    public final BlockPos pos;
    public final boolean bound;
    public final boolean controllerValid;
    public final String channel;
    public final String floorName;
    @Nullable
    public final BlockPos controllerPos;
    public final int priority;
    public final int facesMask;
    public final int strength;
    public final int durationTicks;
    public final int nextDelayTicks;
    public final int arrivalDelayTicks;
    public final List<BoundInterface> interfaces;

    public InterfaceSnapshot(BlockPos pos, boolean bound, boolean controllerValid, String channel, String floorName,
                             @Nullable BlockPos controllerPos, int priority, int facesMask, int strength,
                             int durationTicks, int nextDelayTicks, int arrivalDelayTicks,
                             List<BoundInterface> interfaces) {
        this.pos = pos;
        this.bound = bound;
        this.controllerValid = controllerValid;
        this.channel = channel;
        this.floorName = floorName;
        this.controllerPos = controllerPos;
        this.priority = priority;
        this.facesMask = facesMask;
        this.strength = strength;
        this.durationTicks = durationTicks;
        this.nextDelayTicks = nextDelayTicks;
        this.arrivalDelayTicks = arrivalDelayTicks;
        this.interfaces = List.copyOf(interfaces);
    }

    public static final class BoundInterface {
        public final BlockPos pos;
        public final int priority;
        public final boolean valid;

        public BoundInterface(BlockPos pos, int priority, boolean valid) {
            this.pos = pos;
            this.priority = priority;
            this.valid = valid;
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(bound);
        buf.writeBoolean(controllerValid);
        buf.writeUtf(channel, 64);
        buf.writeUtf(floorName, 64);
        buf.writeBoolean(controllerPos != null);
        if (controllerPos != null) {
            buf.writeBlockPos(controllerPos);
        }
        buf.writeVarInt(priority);
        buf.writeVarInt(facesMask);
        buf.writeVarInt(strength);
        buf.writeVarInt(durationTicks);
        buf.writeVarInt(nextDelayTicks);
        buf.writeVarInt(arrivalDelayTicks);
        buf.writeVarInt(interfaces.size());
        for (BoundInterface ref : interfaces) {
            buf.writeBlockPos(ref.pos);
            buf.writeVarInt(ref.priority);
            buf.writeBoolean(ref.valid);
        }
    }

    public static InterfaceSnapshot decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        boolean bound = buf.readBoolean();
        boolean controllerValid = buf.readBoolean();
        String channel = buf.readUtf(64);
        String floorName = buf.readUtf(64);
        BlockPos controller = buf.readBoolean() ? buf.readBlockPos() : null;
        int priority = buf.readVarInt();
        int facesMask = buf.readVarInt();
        int strength = buf.readVarInt();
        int duration = buf.readVarInt();
        int nextDelay = buf.readVarInt();
        int arrivalDelay = buf.readVarInt();
        int count = Math.min(buf.readVarInt(), MAX_LIST_SIZE);
        List<BoundInterface> interfaces = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            interfaces.add(new BoundInterface(buf.readBlockPos(), buf.readVarInt(), buf.readBoolean()));
        }
        return new InterfaceSnapshot(pos, bound, controllerValid, channel, floorName, controller,
                priority, facesMask, strength, duration, nextDelay, arrivalDelay, interfaces);
    }
}
