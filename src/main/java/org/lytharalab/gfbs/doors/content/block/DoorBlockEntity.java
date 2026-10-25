package org.lytharalab.gfbs.doors.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.lytharalab.gfbs.doors.client.DoorClientSoundHooks;
import org.lytharalab.gfbs.doors.content.DoorCoreRegistry;
import org.lytharalab.gfbs.doors.content.DoorRedstoneMode;
import org.lytharalab.gfbs.doors.content.DoorSpec;
import org.lytharalab.gfbs.doors.content.DoorSpecs;
import org.lytharalab.gfbs.doors.content.ModBlockEntities;
import org.lytharalab.gfbs.doors.content.ModSounds;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DoorBlockEntity extends BlockEntity {
    private static final double SOUND_RANGE = 64.0D;

    public static final byte STATE_CLOSED = 0;
    public static final byte STATE_OPEN = 1;
    public static final byte STATE_CLOSING = 2;
    public static final byte STATE_OPENING = 3;

    private boolean core;
    @Nullable
    private BlockPos corePos;
    private int localX;
    private int localY;
    private int localZ;
    private byte state = STATE_CLOSED;
    private int openTicks;
    private byte skin;
    private DoorRedstoneMode redstoneMode = DoorRedstoneMode.TRIGGER;
    private boolean manualControl = true;
    private boolean redstonePowered;
    @Nullable
    private Boolean pendingOpen;
    private boolean soundLeader = true;
    private transient boolean silentTransition;
    private final Set<BlockPos> activatedBlocks = new HashSet<>();

    public DoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DOOR.get(), pos, state);
        this.core = state.getValue(DoorBlock.CORE);
        this.corePos = pos;
    }

    public DoorSpec getSpec() {
        return ((DoorBlock) getBlockState().getBlock()).spec();
    }

    public boolean isCore() {
        return core;
    }

    public void initCore() {
        this.core = true;
        this.corePos = worldPosition;
        this.state = STATE_CLOSED;
        this.openTicks = 0;
        this.skin = 0;
        this.redstonePowered = false;
        setChanged();
    }

    public void initPart(BlockPos corePos, int x, int y, int z) {
        this.core = false;
        this.corePos = corePos.immutable();
        this.localX = x;
        this.localY = y;
        this.localZ = z;
        setChanged();
    }

    @Nullable
    public DoorBlockEntity getCore() {
        if (core) return this;
        if (level == null || corePos == null) return null;
        BlockEntity be = level.getBlockEntity(corePos);
        if (be instanceof DoorBlockEntity door && door.core) return door;
        return null;
    }

    public Direction getFacing() {
        return getBlockState().getValue(DoorBlock.FACING);
    }

    public byte getState() {
        return state;
    }

    public byte getSkin() {
        return skin;
    }

    public boolean isMoving() {
        return state == STATE_OPENING || state == STATE_CLOSING;
    }

    public boolean isOpenForCollision() {
        return state != STATE_CLOSED;
    }

    public float getOpenTicks(float partialTick) {
        DoorSpec spec = getSpec();
        return switch (state) {
            case STATE_OPEN -> spec.timeToOpen;
            case STATE_OPENING -> Math.min(openTicks + partialTick, spec.timeToOpen);
            case STATE_CLOSING -> Math.max(openTicks - partialTick, 0);
            default -> 0;
        };
    }

    public float getOpenProgress(float partialTick) {
        DoorSpec spec = getSpec();
        return spec.timeToOpen <= 0 ? 0 : getOpenTicks(partialTick) / (float) spec.timeToOpen;
    }

    public boolean toggle(@Nullable Player player) {
        soundLeader = true;
        silentTransition = false;
        return toggleInternal();
    }

    public boolean toggleBlastDoorGroup(@Nullable Player player) {
        if (state != STATE_CLOSED && state != STATE_OPEN) return false;

        List<DoorBlockEntity> group = findBlastDoorGroup();
        for (DoorBlockEntity door : group) {
            boolean leader = door == this;
            door.soundLeader = leader;
            door.silentTransition = !leader;
            door.toggleInternal();
        }
        return true;
    }

    private List<DoorBlockEntity> findBlastDoorGroup() {
        List<DoorBlockEntity> group = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        group.add(this);
        seen.add(worldPosition);
        if (level == null || !"blast_door".equals(getSpec().id)) return group;

        Direction facing = getFacing();
        Direction axis = facing.getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
        for (Direction direction : new Direction[]{axis, axis.getOpposite()}) {
            for (int i = 1; i <= 64; i++) {
                BlockPos pos = worldPosition.relative(direction, i);
                BlockState state = level.getBlockState(pos);
                if (!(state.getBlock() instanceof DoorBlock doorBlock) || !"blast_door".equals(doorBlock.spec().id)) break;
                if (!(level.getBlockEntity(pos) instanceof DoorBlockEntity part)) break;
                DoorBlockEntity core = part.getCore();
                if (core == null || core == this) break;
                if (core.getFacing() != facing || core.getBlockPos().getY() != worldPosition.getY()) break;
                if (!seen.add(core.getBlockPos())) break;
                group.add(core);
            }
        }
        return group;
    }

    public boolean isSoundLeader() {
        return soundLeader;
    }

    private boolean toggleInternal() {
        if (state == STATE_CLOSED) {
            setState(STATE_OPENING);
            return true;
        }
        if (state == STATE_OPEN) {
            setState(STATE_CLOSING);
            return true;
        }
        return false;
    }

    public void cycleSkin() {
        DoorSpec spec = getSpec();
        if (spec.skinCount() <= 1) return;
        skin = (byte) ((skin + 1) % spec.skinCount());
        setChanged();
        sync();
    }

    private void setState(byte newState) {
        if (state == newState) return;
        byte oldState = state;
        state = newState;
        if (newState == STATE_OPENING) openTicks = 0;
        if (newState == STATE_CLOSING) openTicks = getSpec().timeToOpen;

        if (level == null) return;
        if (!level.isClientSide) {
            if (!silentTransition) playStateSound(oldState, newState);
            setChanged();
            sync();
        } else {
            DoorClientSoundHooks.onStateChanged(this, oldState, newState);
        }
        if (newState == STATE_OPEN || newState == STATE_CLOSED) silentTransition = false;
    }

    private void playStateSound(byte oldState, byte newState) {
        DoorSpec spec = getSpec();
        if (newState == STATE_OPENING) {
            playSound(spec.openStart);
            if (spec == DoorSpecs.VAULT_DOOR) playSound(DoorSpecs.SoundKeys.VAULT_SCRAPE);
        } else if (newState == STATE_CLOSING) {
            playSound(spec.closeStart);
        } else if (newState == STATE_OPEN) {
            playSound(spec.openEnd);
        } else if (newState == STATE_CLOSED) {
            playSound(spec.closeEnd);
        }
    }

    private void playSound(String key) {
        SoundEvent event = ModSounds.get(key);
        if (event == null || !(level instanceof ServerLevel serverLevel)) return;
        // Vanilla radius is volume * 16 (blast door 0.5 -> 8 blocks), so push the packet to
        // every player within a fixed range; client-side falloff is the sound's
        // attenuation_distance (64 blocks in sounds.json).
        double x = worldPosition.getX() + 0.5D;
        double y = worldPosition.getY() + 0.5D;
        double z = worldPosition.getZ() + 0.5D;
        ClientboundSoundPacket packet = new ClientboundSoundPacket(
                serverLevel.registryAccess().registryOrThrow(Registries.SOUND_EVENT).wrapAsHolder(event),
                SoundSource.BLOCKS, x, y, z, getSpec().soundVolume, 1F, serverLevel.random.nextLong());
        serverLevel.getServer().getPlayerList().broadcast(null, x, y, z, SOUND_RANGE,
                serverLevel.dimension(), packet);
    }

    public void onNeighborChanged(BlockPos cellPos) {
        if (level == null || level.isClientSide || !core) return;
        BlockPos pos = cellPos.immutable();
        boolean powered = level.hasNeighborSignal(pos);
        boolean contained = activatedBlocks.contains(pos);
        if (!contained && powered) {
            activatedBlocks.add(pos);
            setChanged();
        } else if (contained && !powered) {
            activatedBlocks.remove(pos);
            setChanged();
        }
    }

    public DoorRedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public boolean isManualControl() {
        return manualControl;
    }

    public boolean isRedstonePowered() {
        return redstonePowered;
    }

    public void setRedstoneMode(DoorRedstoneMode mode) {
        if (mode == null || redstoneMode == mode) return;
        redstoneMode = mode;
        pendingOpen = null;
        setChanged();
        sync();
        redstonePowered = !activatedBlocks.isEmpty();
        applyModeTarget(redstonePowered);
    }

    public void setManualControl(boolean manual) {
        if (manualControl == manual) return;
        manualControl = manual;
        setChanged();
        sync();
    }

    private void setConfigRaw(DoorRedstoneMode mode, boolean manual) {
        redstoneMode = mode;
        manualControl = manual;
        pendingOpen = null;
        setChanged();
        sync();
    }

    public void applyConfigToGroup(DoorRedstoneMode mode, boolean manual) {
        if (mode == null) return;
        List<DoorBlockEntity> group = findBlastDoorGroup();
        boolean powered = false;
        for (DoorBlockEntity door : group) {
            powered |= door.redstonePowered;
            door.setConfigRaw(mode, manual);
        }
        applyModeTarget(powered);
    }

    public void syncGroupOnJoin() {
        if (level == null || level.isClientSide || !core) return;
        boolean powered = redstonePowered;
        if (isBlastDoor()) {
            DoorBlockEntity source = null;
            for (DoorBlockEntity door : findBlastDoorGroup()) {
                powered |= door.redstonePowered;
                if (door != this && source == null) {
                    source = door;
                }
            }
            if (source != null) {
                setConfigRaw(source.redstoneMode, source.manualControl);
            }
        }
        if (redstoneMode == DoorRedstoneMode.TRIGGER && powered) {
            setGroupOpenFromRedstone(true);
        } else if (redstoneMode == DoorRedstoneMode.INVERTED && !powered) {
            setGroupOpenFromRedstone(true);
        } else if (redstoneMode == DoorRedstoneMode.GFBS_NORMALLY_OPEN && state == STATE_CLOSED) {
            soundLeader = true;
            silentTransition = false;
            toggleInternal();
        }
    }

    public void scanRedstone() {
        if (level == null || level.isClientSide || !core) return;
        if (!(getBlockState().getBlock() instanceof DoorBlock door)) return;
        activatedBlocks.clear();
        if (level.hasNeighborSignal(worldPosition)) {
            activatedBlocks.add(worldPosition.immutable());
        }
        for (BlockPos part : door.structurePositions(worldPosition, getFacing())) {
            if (level.hasNeighborSignal(part)) {
                activatedBlocks.add(part.immutable());
            }
        }
        redstonePowered = !activatedBlocks.isEmpty();
    }

    private void applyRedstoneState(boolean powered) {
        boolean changed = redstonePowered != powered;
        redstonePowered = powered;
        if (redstoneMode == DoorRedstoneMode.NONE || redstoneMode.isGfbs()) return;
        if (!changed) return;
        switch (redstoneMode) {
            case PULSE -> {
                if (powered) toggleFromRedstone();
            }
            case TRIGGER -> setOpenFromRedstone(powered);
            case INVERTED -> setOpenFromRedstone(!powered);
            default -> {
            }
        }
    }

    private void applyModeTarget(boolean powered) {
        switch (redstoneMode) {
            case TRIGGER -> setOpenFromRedstone(powered);
            case INVERTED -> setOpenFromRedstone(!powered);
            case GFBS_NORMALLY_CLOSED -> setOpenFromRedstone(false);
            case GFBS_NORMALLY_OPEN -> setOpenFromRedstone(true);
            default -> {
            }
        }
    }

    public void applyGateDesiredState(boolean open) {
        if (level == null || level.isClientSide || !core) return;
        if (!redstoneMode.isGfbs()) return;
        setOpenFromRedstone(open);
    }

    public void commandSetOpen(boolean open) {
        if (level == null || level.isClientSide || !core) return;
        if (isBlastDoor()) {
            setGroupOpenFromRedstone(open);
            return;
        }
        setOpenFromRedstone(open);
    }

    public void commandToggle() {
        if (!core) return;
        commandSetOpen(!(state == STATE_OPEN || state == STATE_OPENING));
    }

    private void toggleFromRedstone() {
        if (state != STATE_CLOSED && state != STATE_OPEN) return;
        if (isBlastDoor()) {
            setGroupOpenFromRedstone(state == STATE_CLOSED);
            return;
        }
        soundLeader = true;
        silentTransition = false;
        toggleInternal();
    }

    private boolean isBlastDoor() {
        return "blast_door".equals(getSpec().id);
    }

    private void setGroupOpenFromRedstone(boolean open) {
        for (DoorBlockEntity door : findBlastDoorGroup()) {
            boolean leader = door == this;
            door.soundLeader = leader;
            door.silentTransition = !leader;
            if (open && door.state == STATE_CLOSED) {
                door.toggleInternal();
            } else if (!open && door.state == STATE_OPEN) {
                door.toggleInternal();
            }
        }
    }

    private void setOpenFromRedstone(boolean open) {
        if (state == STATE_OPENING || state == STATE_CLOSING) {
            boolean headingOpen = state == STATE_OPENING;
            pendingOpen = open == headingOpen ? null : open;
            return;
        }
        if (isBlastDoor()) {
            setGroupOpenFromRedstone(open);
            return;
        }
        if (open && state == STATE_CLOSED) {
            toggleFromRedstone();
        } else if (!open && state == STATE_OPEN) {
            toggleFromRedstone();
        }
    }

    private void applyPendingOpen() {
        if (pendingOpen == null) return;
        boolean open = pendingOpen;
        pendingOpen = null;
        if (isBlastDoor()) {
            setGroupOpenFromRedstone(open);
            return;
        }
        if (open && state == STATE_CLOSED) {
            toggleFromRedstone();
        } else if (!open && state == STATE_OPEN) {
            toggleFromRedstone();
        }
    }

    public void tick() {
        if (level == null || !core) return;
        DoorSpec spec = getSpec();

        boolean justFinished = false;
        if (state == STATE_OPENING) {
            if (openTicks < spec.timeToOpen) openTicks++;
            if (openTicks >= spec.timeToOpen) {
                openTicks = spec.timeToOpen;
                setState(STATE_OPEN);
                justFinished = true;
            }
        } else if (state == STATE_CLOSING) {
            if (openTicks > 0) openTicks--;
            if (openTicks <= 0) {
                openTicks = 0;
                setState(STATE_CLOSED);
                justFinished = true;
            }
        }

        if (level.isClientSide) {
            DoorClientSoundHooks.tick(this);
            return;
        }

        if (spec == DoorSpecs.VAULT_DOOR) tickVaultSounds();
        if (justFinished) applyPendingOpen();
        tickRedstone();
    }

    private void tickVaultSounds() {
        if (state == STATE_OPENING) {
            if (openTicks >= 45 && openTicks <= 115 && (openTicks - 45) % 10 == 0) {
                playSound(DoorSpecs.SoundKeys.VAULT_THUD);
            }
        } else if (state == STATE_CLOSING) {
            if (openTicks == 30) playSound(DoorSpecs.SoundKeys.VAULT_SCRAPE);
            if (openTicks >= 45 && openTicks <= 115 && (openTicks - 45) % 10 == 0) {
                playSound(DoorSpecs.SoundKeys.VAULT_THUD);
            }
        }
    }

    private void tickRedstone() {
        applyRedstoneState(!activatedBlocks.isEmpty());
    }

    public void sync() {
        if (level == null || level.isClientSide) return;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public void onLoad() {
        if (core) {
            DoorCoreRegistry.register(this);
        }
    }

    @Override
    public void setRemoved() {
        DoorCoreRegistry.unregister(this);
        if (level != null && level.isClientSide) DoorClientSoundHooks.stop(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null && level.isClientSide) DoorClientSoundHooks.stop(this);
        super.onChunkUnloaded();
    }

    public void removeStructure() {
        if (level == null || level.isClientSide) return;
        Block block = getBlockState().getBlock();
        if (!(block instanceof DoorBlock door)) return;
        Direction facing = getFacing();
        for (BlockPos pos : door.structurePositions(worldPosition, facing)) {
            if (level.getBlockState(pos).getBlock() == block) {
                level.removeBlock(pos, false);
            }
        }
        level.removeBlock(worldPosition, false);
    }

    @Override
    public AABB getRenderBoundingBox() {
        DoorSpec spec = getSpec();
        if ("transition_seal".equals(spec.id)) return new AABB(worldPosition).inflate(64D);
        int extent = 1;
        for (int d : spec.dimensions) extent = Math.max(extent, d);
        for (int[] extra : spec.extraDimensions) {
            for (int d : extra) extent = Math.max(extent, d);
        }
        return new AABB(worldPosition).inflate(extent + 8);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("core", core);
        tag.putByte("state", state);
        tag.putInt("openTicks", openTicks);
        tag.putByte("skin", skin);
        tag.putByte("redstoneMode", (byte) redstoneMode.ordinal());
        tag.putBoolean("manualControl", manualControl);
        tag.putBoolean("redstonePowered", redstonePowered);
        tag.putByte("pendingOpen", (byte) (pendingOpen == null ? -1 : (pendingOpen ? 1 : 0)));
        tag.putBoolean("soundLeader", soundLeader);
        tag.putInt("localX", localX);
        tag.putInt("localY", localY);
        tag.putInt("localZ", localZ);
        if (corePos != null) {
            tag.putInt("coreX", corePos.getX());
            tag.putInt("coreY", corePos.getY());
            tag.putInt("coreZ", corePos.getZ());
        }
        ListTag powered = new ListTag();
        for (BlockPos pos : activatedBlocks) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("x", pos.getX());
            entry.putInt("y", pos.getY());
            entry.putInt("z", pos.getZ());
            powered.add(entry);
        }
        tag.put("powered", powered);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        core = tag.getBoolean("core");
        state = tag.getByte("state");
        openTicks = tag.getInt("openTicks");
        skin = tag.getByte("skin");
        redstoneMode = DoorRedstoneMode.byOrdinal(tag.getByte("redstoneMode"));
        manualControl = !tag.contains("manualControl") || tag.getBoolean("manualControl");
        redstonePowered = tag.getBoolean("redstonePowered");
        if (tag.contains("pendingOpen")) {
            byte pending = tag.getByte("pendingOpen");
            pendingOpen = pending < 0 ? null : pending > 0;
        } else {
            pendingOpen = null;
        }
        soundLeader = !tag.contains("soundLeader") || tag.getBoolean("soundLeader");
        localX = tag.getInt("localX");
        localY = tag.getInt("localY");
        localZ = tag.getInt("localZ");
        if (tag.contains("coreX")) {
            corePos = new BlockPos(tag.getInt("coreX"), tag.getInt("coreY"), tag.getInt("coreZ"));
        } else {
            corePos = worldPosition;
        }
        activatedBlocks.clear();
        ListTag powered = tag.getList("powered", Tag.TAG_COMPOUND);
        for (int i = 0; i < powered.size(); i++) {
            CompoundTag entry = powered.getCompound(i);
            activatedBlocks.add(new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z")));
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) {
            byte old = state;
            load(packet.getTag());
            if (level != null && level.isClientSide && old != state) {
                DoorClientSoundHooks.onStateChanged(this, old, state);
            }
        }
    }
}
