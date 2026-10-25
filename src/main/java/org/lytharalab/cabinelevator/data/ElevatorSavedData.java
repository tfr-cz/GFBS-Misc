package org.lytharalab.cabinelevator.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class ElevatorSavedData extends SavedData {
    public static final int DEFAULT_TIME_TICKS = 40;

    private static final String DATA_NAME = "cabin_elevator";
    private static final Comparator<DimPos> FLOOR_ORDER = Comparator
            .comparingInt((DimPos p) -> p.pos().getY())
            .thenComparingInt(p -> p.pos().getX())
            .thenComparingInt(p -> p.pos().getZ());

    public static final class ChannelSetting {
        public RangeBox range = RangeBox.DEFAULT;
        public int timeTicks = DEFAULT_TIME_TICKS;
    }

    public static final class ControllerEntry {
        public String channel = "";
        public String floorName = "";
        public int draftTimeTicks = DEFAULT_TIME_TICKS;
    }

    public static final class InterfaceEntry {
        @Nullable
        public DimPos controller;
        public int priority = 1;
        public int facesMask = 1 << Direction.NORTH.get3DDataValue();
        public int strength = 15;
        public int durationTicks = 20;
        public int nextDelayTicks = 20;
        public int arrivalDelayTicks = 20;
    }

    public static final class CallerEntry {
        @Nullable
        public DimPos controller;
    }

    private record CurrentKey(ResourceKey<Level> dimension, String channel) {
    }

    private final Map<DimPos, ControllerEntry> controllers = new HashMap<>();
    private final Map<String, ChannelSetting> channels = new HashMap<>();
    private final Map<DimPos, InterfaceEntry> interfaces = new HashMap<>();
    private final Map<DimPos, CallerEntry> callers = new HashMap<>();
    private final Map<CurrentKey, DimPos> currentFloors = new HashMap<>();

    private ElevatorSavedData() {
    }

    public static ElevatorSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(ElevatorSavedData::load, ElevatorSavedData::new, DATA_NAME);
    }

    public static ElevatorSavedData load(CompoundTag tag) {
        ElevatorSavedData data = new ElevatorSavedData();

        ListTag channelTags = tag.getList("channels", Tag.TAG_COMPOUND);
        for (int i = 0; i < channelTags.size(); i++) {
            CompoundTag c = channelTags.getCompound(i);
            String name = c.getString("name");
            if (name.isBlank()) {
                continue;
            }
            ChannelSetting setting = new ChannelSetting();
            if (c.contains("range", Tag.TAG_COMPOUND)) {
                setting.range = RangeBox.load(c.getCompound("range"));
            } else {
                setting.range = RangeBox.legacy(Math.max(1, c.getInt("width")),
                        Math.max(1, c.getInt("height")), Math.max(1, c.getInt("depth")));
            }
            setting.timeTicks = Math.max(0, c.getInt("timeTicks"));
            data.channels.put(name, setting);
        }

        ListTag controllerTags = tag.getList("controllers", Tag.TAG_COMPOUND);
        for (int i = 0; i < controllerTags.size(); i++) {
            CompoundTag c = controllerTags.getCompound(i);
            DimPos key = DimPos.load(c);
            if (key == null) {
                continue;
            }
            ControllerEntry entry = new ControllerEntry();
            entry.channel = c.getString("channel");
            entry.floorName = c.getString("floorName");
            entry.draftTimeTicks = Math.max(0, c.getInt("draftTimeTicks"));
            data.controllers.put(key, entry);
            if (!entry.channel.isBlank() && !data.channels.containsKey(entry.channel)) {
                ChannelSetting setting = new ChannelSetting();
                setting.timeTicks = entry.draftTimeTicks;
                data.channels.put(entry.channel, setting);
            }
        }

        ListTag interfaceTags = tag.getList("interfaces", Tag.TAG_COMPOUND);
        for (int i = 0; i < interfaceTags.size(); i++) {
            CompoundTag c = interfaceTags.getCompound(i);
            DimPos key = DimPos.load(c);
            if (key == null) {
                continue;
            }
            InterfaceEntry entry = new InterfaceEntry();
            if (c.contains("controller", Tag.TAG_COMPOUND)) {
                entry.controller = DimPos.load(c.getCompound("controller"));
            }
            entry.priority = Math.max(1, c.getInt("priority"));
            if (c.contains("faces")) {
                entry.facesMask = c.getInt("faces") & 63;
            } else {
                entry.facesMask = 1 << readDirection(c.getInt("face")).get3DDataValue();
            }
            if (entry.facesMask == 0) {
                entry.facesMask = 1 << Direction.NORTH.get3DDataValue();
            }
            entry.strength = Math.max(0, Math.min(15, c.getInt("strength")));
            entry.durationTicks = Math.max(0, c.getInt("durationTicks"));
            entry.nextDelayTicks = Math.max(0, c.getInt("nextDelayTicks"));
            entry.arrivalDelayTicks = Math.max(0, c.getInt("arrivalDelayTicks"));
            data.interfaces.put(key, entry);
        }

        ListTag callerTags = tag.getList("callers", Tag.TAG_COMPOUND);
        for (int i = 0; i < callerTags.size(); i++) {
            CompoundTag c = callerTags.getCompound(i);
            DimPos key = DimPos.load(c);
            if (key == null) {
                continue;
            }
            CallerEntry entry = new CallerEntry();
            if (c.contains("controller", Tag.TAG_COMPOUND)) {
                entry.controller = DimPos.load(c.getCompound("controller"));
            }
            data.callers.put(key, entry);
        }

        ListTag currentTags = tag.getList("currentFloors", Tag.TAG_COMPOUND);
        for (int i = 0; i < currentTags.size(); i++) {
            CompoundTag c = currentTags.getCompound(i);
            String channel = c.getString("channel");
            DimPos floor = DimPos.load(c);
            if (floor == null || channel.isBlank()) {
                continue;
            }
            data.currentFloors.put(new CurrentKey(floor.dimension(), channel), floor);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag channelTags = new ListTag();
        for (Map.Entry<String, ChannelSetting> e : channels.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putString("name", e.getKey());
            c.put("range", e.getValue().range.save());
            c.putInt("timeTicks", e.getValue().timeTicks);
            channelTags.add(c);
        }
        tag.put("channels", channelTags);

        ListTag controllerTags = new ListTag();
        for (Map.Entry<DimPos, ControllerEntry> e : controllers.entrySet()) {
            CompoundTag c = e.getKey().save();
            ControllerEntry entry = e.getValue();
            c.putString("channel", entry.channel);
            c.putString("floorName", entry.floorName);
            c.putInt("draftTimeTicks", entry.draftTimeTicks);
            controllerTags.add(c);
        }
        tag.put("controllers", controllerTags);

        ListTag interfaceTags = new ListTag();
        for (Map.Entry<DimPos, InterfaceEntry> e : interfaces.entrySet()) {
            CompoundTag c = e.getKey().save();
            InterfaceEntry entry = e.getValue();
            if (entry.controller != null) {
                c.put("controller", entry.controller.save());
            }
            c.putInt("priority", entry.priority);
            c.putInt("faces", entry.facesMask);
            c.putInt("strength", entry.strength);
            c.putInt("durationTicks", entry.durationTicks);
            c.putInt("nextDelayTicks", entry.nextDelayTicks);
            c.putInt("arrivalDelayTicks", entry.arrivalDelayTicks);
            interfaceTags.add(c);
        }
        tag.put("interfaces", interfaceTags);

        ListTag callerTags = new ListTag();
        for (Map.Entry<DimPos, CallerEntry> e : callers.entrySet()) {
            CompoundTag c = e.getKey().save();
            if (e.getValue().controller != null) {
                c.put("controller", e.getValue().controller.save());
            }
            callerTags.add(c);
        }
        tag.put("callers", callerTags);

        ListTag currentTags = new ListTag();
        for (Map.Entry<CurrentKey, DimPos> e : currentFloors.entrySet()) {
            CompoundTag c = e.getValue().save();
            c.putString("channel", e.getKey().channel());
            currentTags.add(c);
        }
        tag.put("currentFloors", currentTags);
        return tag;
    }

    public ControllerEntry getOrCreateController(DimPos key) {
        ControllerEntry entry = controllers.get(key);
        if (entry == null) {
            entry = new ControllerEntry();
            controllers.put(key, entry);
            setDirty();
        }
        return entry;
    }

    @Nullable
    public ControllerEntry controller(DimPos key) {
        return controllers.get(key);
    }

    public Map<DimPos, ControllerEntry> controllers() {
        return controllers;
    }

    @Nullable
    public ChannelSetting channel(String name) {
        return channels.get(name);
    }

    public Set<String> channelNames() {
        return new TreeSet<>(channels.keySet());
    }

    public RangeBox effectiveRange(ControllerEntry entry) {
        ChannelSetting setting = entry.channel.isBlank() ? null : channels.get(entry.channel);
        return setting != null ? setting.range : RangeBox.DEFAULT;
    }

    public int effectiveTime(ControllerEntry entry) {
        ChannelSetting setting = entry.channel.isBlank() ? null : channels.get(entry.channel);
        return setting != null ? setting.timeTicks : entry.draftTimeTicks;
    }

    public List<DimPos> floors(ResourceKey<Level> dimension, String channel) {
        if (channel == null || channel.isBlank()) {
            return List.of();
        }
        List<DimPos> result = new ArrayList<>();
        for (Map.Entry<DimPos, ControllerEntry> e : controllers.entrySet()) {
            if (e.getKey().dimension().equals(dimension) && channel.equals(e.getValue().channel)) {
                result.add(e.getKey());
            }
        }
        result.sort(FLOOR_ORDER);
        return result;
    }

    public int floorIndex(ResourceKey<Level> dimension, String channel, BlockPos pos) {
        List<DimPos> floors = floors(dimension, channel);
        for (int i = 0; i < floors.size(); i++) {
            if (floors.get(i).pos().equals(pos)) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    public DimPos currentFloor(ResourceKey<Level> dimension, String channel) {
        if (channel == null || channel.isBlank()) {
            return null;
        }
        CurrentKey key = new CurrentKey(dimension, channel);
        DimPos current = currentFloors.get(key);
        List<DimPos> floors = floors(dimension, channel);
        if (current != null && floors.contains(current)) {
            return current;
        }
        if (floors.isEmpty()) {
            currentFloors.remove(key);
            setDirty();
            return null;
        }
        DimPos fallback = floors.get(0);
        currentFloors.put(key, fallback);
        setDirty();
        return fallback;
    }

    public void setCurrentFloor(ResourceKey<Level> dimension, String channel, BlockPos pos) {
        if (channel == null || channel.isBlank()) {
            return;
        }
        DimPos floor = new DimPos(dimension, pos.immutable());
        if (!floors(dimension, channel).contains(floor)) {
            return;
        }
        currentFloors.put(new CurrentKey(dimension, channel), floor);
        setDirty();
    }

    public void removeController(DimPos key) {
        ControllerEntry entry = controllers.remove(key);
        if (entry == null) {
            return;
        }
        for (InterfaceEntry iface : interfaces.values()) {
            if (key.equals(iface.controller)) {
                iface.controller = null;
            }
        }
        for (CallerEntry caller : callers.values()) {
            if (key.equals(caller.controller)) {
                caller.controller = null;
            }
        }
        currentFloors.values().removeIf(floor -> floor.equals(key));
        cleanupChannel(entry.channel);
        setDirty();
    }

    public void setControllerConfig(DimPos key, String newChannel, String floorName, int timeTicks) {
        ControllerEntry entry = getOrCreateController(key);
        String oldChannel = entry.channel;
        entry.channel = newChannel;
        entry.floorName = floorName;
        entry.draftTimeTicks = timeTicks;

        if (!newChannel.isBlank()) {
            ChannelSetting setting = channels.computeIfAbsent(newChannel, name -> new ChannelSetting());
            setting.timeTicks = timeTicks;
        }

        if (!oldChannel.equals(newChannel)) {
            CurrentKey oldKey = new CurrentKey(key.dimension(), oldChannel);
            DimPos current = currentFloors.get(oldKey);
            if (key.equals(current)) {
                currentFloors.remove(oldKey);
            }
            cleanupChannel(oldChannel);
        }
        currentFloor(key.dimension(), newChannel);
        setDirty();
    }

    public void setChannelRange(String channel, RangeBox range) {
        if (channel == null || channel.isBlank() || range == null || !range.isValid()) {
            return;
        }
        ChannelSetting setting = channels.get(channel);
        if (setting != null) {
            setting.range = range;
            setDirty();
        }
    }

    private void cleanupChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return;
        }
        for (ControllerEntry entry : controllers.values()) {
            if (channel.equals(entry.channel)) {
                return;
            }
        }
        channels.remove(channel);
        currentFloors.keySet().removeIf(k -> k.channel().equals(channel));
        setDirty();
    }

    public InterfaceEntry getOrCreateInterface(DimPos key, int defaultFacesMask) {
        InterfaceEntry entry = interfaces.get(key);
        if (entry == null) {
            entry = new InterfaceEntry();
            entry.facesMask = defaultFacesMask & 63;
            if (entry.facesMask == 0) {
                entry.facesMask = 1 << Direction.NORTH.get3DDataValue();
            }
            interfaces.put(key, entry);
            setDirty();
        }
        return entry;
    }

    @Nullable
    public InterfaceEntry interfaceAt(DimPos key) {
        return interfaces.get(key);
    }

    public void removeInterface(DimPos key) {
        if (interfaces.remove(key) != null) {
            setDirty();
        }
    }

    public List<DimPos> interfacesBoundTo(DimPos controller) {
        List<DimPos> result = new ArrayList<>();
        for (Map.Entry<DimPos, InterfaceEntry> e : interfaces.entrySet()) {
            if (controller.equals(e.getValue().controller)) {
                result.add(e.getKey());
            }
        }
        result.sort(Comparator
                .comparingInt((DimPos p) -> interfaces.get(p).priority)
                .thenComparingInt(p -> p.pos().getY())
                .thenComparingInt(p -> p.pos().getX())
                .thenComparingInt(p -> p.pos().getZ()));
        return result;
    }

    public CallerEntry getOrCreateCaller(DimPos key) {
        CallerEntry entry = callers.get(key);
        if (entry == null) {
            entry = new CallerEntry();
            callers.put(key, entry);
            setDirty();
        }
        return entry;
    }

    @Nullable
    public CallerEntry callerAt(DimPos key) {
        return callers.get(key);
    }

    public void removeCaller(DimPos key) {
        if (callers.remove(key) != null) {
            setDirty();
        }
    }

    @Nullable
    public DimPos callerFor(DimPos controller) {
        for (Map.Entry<DimPos, CallerEntry> e : callers.entrySet()) {
            if (controller.equals(e.getValue().controller)) {
                return e.getKey();
            }
        }
        return null;
    }

    public int floorCount(String channel) {
        int count = 0;
        for (ControllerEntry entry : controllers.values()) {
            if (channel.equals(entry.channel)) {
                count++;
            }
        }
        return count;
    }

    private static Direction readDirection(int index) {
        Direction[] values = Direction.values();
        if (index < 0 || index >= values.length) {
            return Direction.NORTH;
        }
        return values[index];
    }
}
