package org.lytharalab.gfbs.doors.content.block;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.lytharalab.gfbs.doors.client.DoorClientSoundHooks;
import org.lytharalab.gfbs.doors.content.DoorCoreRegistry;
import org.lytharalab.gfbs.doors.content.DoorRedstoneMode;
import org.lytharalab.gfbs.doors.content.DoorSpec;
import org.lytharalab.gfbs.doors.content.ModBlockEntities;
import org.lytharalab.gfbs.doors.content.ModSounds;

public class DoorBlockEntity extends BlockEntity {
    private static final double SOUND_RANGE = 64.0;
    public static final int MAX_EXTRA_HEIGHT = 12;
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
    private byte state = 0;
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
    private int extraHeight;
    private DoorSpec cachedSpec;

    public DoorBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)ModBlockEntities.DOOR.get(), pos, state);
        this.core = (Boolean)state.getValue(DoorBlock.CORE);
        this.corePos = pos;
    }

    public DoorSpec getSpec() {
        DoorSpec spec = this.cachedSpec;
        if (spec == null) {
            spec = ((DoorBlock)this.getBlockState().getBlock()).spec();
            this.cachedSpec = spec;
        }
        return spec;
    }

    public boolean isCore() {
        return this.core;
    }

    public void initCore() {
        this.core = true;
        this.corePos = this.worldPosition;
        this.state = 0;
        this.openTicks = 0;
        this.skin = 0;
        this.redstonePowered = false;
        this.extraHeight = 0;
        this.setChanged();
    }

    public void initPart(BlockPos corePos, int x, int y, int z) {
        this.initPart(corePos, x, y, z, 0);
    }

    public void initPart(BlockPos corePos, int x, int y, int z, int extraHeight) {
        this.core = false;
        this.corePos = corePos.immutable();
        this.localX = x;
        this.localY = y;
        this.localZ = z;
        this.extraHeight = clampExtraHeight(extraHeight);
        this.setChanged();
    }

    public int getExtraHeight() {
        return this.isBlastDoor() ? clampExtraHeight(this.extraHeight) : 0;
    }

    public void setExtraHeight(int value) {
        this.extraHeight = clampExtraHeight(value);
    }

    public static int clampExtraHeight(int value) {
        return Mth.clamp(value, 0, 12);
    }

    @Nullable
    public DoorBlockEntity getCore() {
        if (this.core) {
            return this;
        } else if (this.level != null && this.corePos != null) {
            if (this.level.getBlockEntity(this.corePos) instanceof DoorBlockEntity door && door.core) {
                return door;
            }

            return null;
        } else {
            return null;
        }
    }

    public Direction getFacing() {
        return (Direction)this.getBlockState().getValue(DoorBlock.FACING);
    }

    public byte getState() {
        return this.state;
    }

    public byte getSkin() {
        return this.skin;
    }

    public boolean isMoving() {
        return this.state == 3 || this.state == 2;
    }

    public boolean isOpenForCollision() {
        return this.state != 0;
    }

    public int getTimeToOpen() {
        int base = this.getSpec().timeToOpen;
        int extra = this.getExtraHeight();
        return base > 0 && extra > 0 && this.isBlastDoor() ? Math.max(1, Math.round((float)base * (5.0F + (float)extra) / 5.0F)) : base;
    }

    public float getOpenTicks(float partialTick) {
        int duration = this.getTimeToOpen();

        return switch (this.state) {
            case 1 -> (float)duration;
            case 2 -> Math.max((float)this.openTicks - partialTick, 0.0F);
            case 3 -> Math.min((float)this.openTicks + partialTick, (float)duration);
            default -> 0.0F;
        };
    }

    public float getOpenProgress(float partialTick) {
        int duration = this.getTimeToOpen();
        return duration <= 0 ? 0.0F : this.getOpenTicks(partialTick) / (float)duration;
    }

    public boolean toggle(@Nullable Player player) {
        this.soundLeader = true;
        this.silentTransition = false;
        return this.toggleInternal();
    }

    public boolean toggleBlastDoorGroup(@Nullable Player player) {
        if (this.state != 0 && this.state != 1) {
            return false;
        } else {
            for (DoorBlockEntity door : this.findBlastDoorGroup()) {
                boolean leader = door == this;
                door.soundLeader = leader;
                door.silentTransition = !leader;
                door.toggleInternal();
            }

            return true;
        }
    }

    private List<DoorBlockEntity> findBlastDoorGroup() {
        List<DoorBlockEntity> group = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        group.add(this);
        seen.add(this.worldPosition);
        if (this.level != null && "blast_door".equals(this.getSpec().id)) {
            Direction facing = this.getFacing();
            Direction axis = facing.getAxis() == Axis.Z ? Direction.EAST : Direction.SOUTH;

            for (Direction direction : new Direction[]{axis, axis.getOpposite()}) {
                for (int i = 1; i <= 64; i++) {
                    BlockPos pos = this.worldPosition.relative(direction, i);
                    BlockState state = this.level.getBlockState(pos);
                    if (!(state.getBlock() instanceof DoorBlock doorBlock)
                        || !"blast_door".equals(doorBlock.spec().id)
                        || !(this.level.getBlockEntity(pos) instanceof DoorBlockEntity part)) {
                        break;
                    }

                    DoorBlockEntity core = part.getCore();
                    if (core == null
                        || core == this
                        || core.getFacing() != facing
                        || core.getBlockPos().getY() != this.worldPosition.getY()
                        || !seen.add(core.getBlockPos())) {
                        break;
                    }

                    group.add(core);
                }
            }

            return group;
        } else {
            return group;
        }
    }

    public boolean isSoundLeader() {
        return this.soundLeader;
    }

    private boolean toggleInternal() {
        if (this.state == 0) {
            this.setState((byte)3);
            return true;
        } else if (this.state == 1) {
            this.setState((byte)2);
            return true;
        } else {
            return false;
        }
    }

    public void cycleSkin() {
        DoorSpec spec = this.getSpec();
        if (spec.skinCount() > 1) {
            this.skin = (byte)((this.skin + 1) % spec.skinCount());
            this.setChanged();
            this.sync();
        }
    }

    private void setState(byte newState) {
        if (this.state != newState) {
            byte oldState = this.state;
            this.state = newState;
            if (newState == 3) {
                this.openTicks = 0;
            }

            if (newState == 2) {
                this.openTicks = this.getTimeToOpen();
            }

            if (this.level != null) {
                if (!this.level.isClientSide) {
                    if (!this.silentTransition) {
                        this.playStateSound(oldState, newState);
                    }

                    this.setChanged();
                    this.sync();
                } else {
                    DoorClientSoundHooks.onStateChanged(this, oldState, newState);
                }

                if (newState == 1 || newState == 0) {
                    this.silentTransition = false;
                }
            }
        }
    }

    private void playStateSound(byte oldState, byte newState) {
        DoorSpec spec = this.getSpec();
        if (newState == 3) {
            this.playSound(spec.openStart);
            if (spec.inheritsFrom("vault_door")) {
                this.playSound("block.vault_scrape_new");
            }
        } else if (newState == 2) {
            this.playSound(spec.closeStart);
        } else if (newState == 1) {
            this.playSound(spec.openEnd);
        } else if (newState == 0) {
            this.playSound(spec.closeEnd);
        }
    }

    private void playSound(String key) {
        SoundEvent event = ModSounds.get(key);
        if (event != null && this.level instanceof ServerLevel serverLevel) {
            double var11 = (double)this.worldPosition.getX() + 0.5;
            double y = (double)this.worldPosition.getY() + 0.5;
            double z = (double)this.worldPosition.getZ() + 0.5;
            ClientboundSoundPacket packet = new ClientboundSoundPacket(
                serverLevel.registryAccess().registryOrThrow(Registries.SOUND_EVENT).wrapAsHolder(event),
                SoundSource.BLOCKS,
                var11,
                y,
                z,
                Mth.clamp(this.getSpec().soundVolume, 0.0F, 1.0F),
                1.0F,
                serverLevel.random.nextLong()
            );
            serverLevel.getServer().getPlayerList().broadcast(null, var11, y, z, 64.0, serverLevel.dimension(), packet);
        }
    }

    public void onNeighborChanged(BlockPos cellPos) {
        if (this.level != null && !this.level.isClientSide && this.core) {
            BlockPos pos = cellPos.immutable();
            boolean powered = this.level.hasNeighborSignal(pos);
            boolean contained = this.activatedBlocks.contains(pos);
            if (!contained && powered) {
                this.activatedBlocks.add(pos);
                this.setChanged();
            } else if (contained && !powered) {
                this.activatedBlocks.remove(pos);
                this.setChanged();
            }
        }
    }

    public DoorRedstoneMode getRedstoneMode() {
        return this.redstoneMode;
    }

    public boolean isManualControl() {
        return this.manualControl;
    }

    public boolean isRedstonePowered() {
        return this.redstonePowered;
    }

    public void setRedstoneMode(DoorRedstoneMode mode) {
        if (mode != null && this.redstoneMode != mode) {
            this.redstoneMode = mode;
            this.pendingOpen = null;
            this.setChanged();
            this.sync();
            this.redstonePowered = !this.activatedBlocks.isEmpty();
            this.applyModeTarget(this.redstonePowered);
        }
    }

    public void setManualControl(boolean manual) {
        if (this.manualControl != manual) {
            this.manualControl = manual;
            this.setChanged();
            this.sync();
        }
    }

    private void setConfigRaw(DoorRedstoneMode mode, boolean manual) {
        this.redstoneMode = mode;
        this.manualControl = manual;
        this.pendingOpen = null;
        this.setChanged();
        this.sync();
    }

    public void applyConfigToGroup(DoorRedstoneMode mode, boolean manual) {
        this.applyConfigToGroup(mode, manual, this.getExtraHeight());
    }

    public boolean applyConfigToGroup(DoorRedstoneMode mode, boolean manual, int desiredExtraHeight) {
        if (mode == null) {
            return false;
        } else {
            List<DoorBlockEntity> group = this.findBlastDoorGroup();
            boolean powered = false;

            for (DoorBlockEntity door : group) {
                powered |= door.redstonePowered;
                door.setConfigRaw(mode, manual);
            }

            this.applyModeTarget(powered);
            return !this.isBlastDoor() ? true : this.applyBlastHeightToGroup(desiredExtraHeight);
        }
    }

    public boolean applyBlastHeightToGroup(int desiredExtraHeight) {
        if (this.level != null && !this.level.isClientSide && this.core && this.isBlastDoor()) {
            int target = clampExtraHeight(desiredExtraHeight);
            List<DoorBlockEntity> group = this.findBlastDoorGroup();
            boolean changed = false;

            for (DoorBlockEntity door : group) {
                if (door.extraHeight != target) {
                    changed = true;
                    break;
                }
            }

            if (!changed) {
                return true;
            } else {
                for (DoorBlockEntity doorx : group) {
                    if (!doorx.canResizeTo(target)) {
                        return false;
                    }
                }

                for (DoorBlockEntity doorxx : group) {
                    doorxx.resizeStructure(target);
                }

                return true;
            }
        } else {
            return true;
        }
    }

    private boolean canResizeTo(int target) {
        if (this.level == null) {
            return false;
        } else if (target <= this.extraHeight) {
            return true;
        } else if (this.getBlockState().getBlock() instanceof DoorBlock door) {
            Direction var7 = this.getFacing();
            Set<BlockPos> oldParts = door.structurePositions(this.worldPosition, var7, this.extraHeight);

            for (BlockPos pos : door.structurePositions(this.worldPosition, var7, target)) {
                if (!oldParts.contains(pos)) {
                    if (this.level.isOutsideBuildHeight(pos)) {
                        return false;
                    }

                    if (!this.level.getBlockState(pos).canBeReplaced()) {
                        return false;
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private void resizeStructure(int target) {
        if (this.level != null && !this.level.isClientSide) {
            target = clampExtraHeight(target);
            if (target != this.extraHeight && this.getBlockState().getBlock() instanceof DoorBlock door) {
                Direction var12 = this.getFacing();
                Set<BlockPos> oldParts = door.structurePositions(this.worldPosition, var12, this.extraHeight);
                Set<BlockPos> newParts = door.structurePositions(this.worldPosition, var12, target);

                for (BlockPos pos : oldParts) {
                    if (!newParts.contains(pos)) {
                        this.removePart(pos, door);
                    }
                }

                this.extraHeight = target;
                int duration = this.getTimeToOpen();
                if (this.state == 1) {
                    this.openTicks = duration;
                } else {
                    this.openTicks = Mth.clamp(this.openTicks, 0, duration);
                }

                for (BlockPos posx : newParts) {
                    if (!oldParts.contains(posx)) {
                        this.placePart(posx, door, var12);
                    }
                }

                for (BlockPos posxx : newParts) {
                    BlockEntity var10 = this.level.getBlockEntity(posxx);
                    if (var10 instanceof DoorBlockEntity) {
                        DoorBlockEntity part = (DoorBlockEntity)var10;
                        if (!part.isCore()) {
                            part.extraHeight = target;
                            part.setChanged();
                        }
                    }
                }

                this.setChanged();
                this.sync();
            }
        }
    }

    private void placePart(BlockPos pos, DoorBlock door, Direction facing) {
        BlockState partState = (BlockState)((BlockState)this.getBlockState().getBlock().defaultBlockState().setValue(DoorBlock.FACING, facing))
            .setValue(DoorBlock.CORE, false);
        this.level.setBlock(pos, partState, 3);
        if (this.level.getBlockEntity(pos) instanceof DoorBlockEntity part) {
            part.initPart(
                this.worldPosition,
                pos.getX() - this.worldPosition.getX(),
                pos.getY() - this.worldPosition.getY(),
                pos.getZ() - this.worldPosition.getZ(),
                this.extraHeight
            );
        }

        this.level.sendBlockUpdated(pos, partState, partState, 2);
    }

    private void removePart(BlockPos pos, DoorBlock door) {
        if (this.level.getBlockState(pos).getBlock() == door && this.level.getBlockEntity(pos) instanceof DoorBlockEntity part) {
            DoorBlockEntity partCore = part.getCore();
            if (partCore == this) {
                this.level.removeBlock(pos, false);
            }
        }
    }

    public void syncGroupOnJoin() {
        if (this.level != null && !this.level.isClientSide && this.core) {
            boolean powered = this.redstonePowered;
            if (this.isBlastDoor()) {
                DoorBlockEntity source = null;

                for (DoorBlockEntity door : this.findBlastDoorGroup()) {
                    powered |= door.redstonePowered;
                    if (door != this && source == null) {
                        source = door;
                    }
                }

                if (source != null) {
                    this.setConfigRaw(source.redstoneMode, source.manualControl);
                    if (this.getExtraHeight() != source.getExtraHeight()) {
                        this.applyBlastHeightToGroup(source.getExtraHeight());
                    }
                }
            }

            if (this.redstoneMode == DoorRedstoneMode.TRIGGER && powered) {
                this.setGroupOpenFromRedstone(true);
            } else if (this.redstoneMode == DoorRedstoneMode.INVERTED && !powered) {
                this.setGroupOpenFromRedstone(true);
            } else if (this.redstoneMode == DoorRedstoneMode.GFBS_NORMALLY_OPEN && this.state == 0) {
                this.soundLeader = true;
                this.silentTransition = false;
                this.toggleInternal();
            }
        }
    }

    public void scanRedstone() {
        if (this.level != null && !this.level.isClientSide && this.core && this.getBlockState().getBlock() instanceof DoorBlock door) {
            this.activatedBlocks.clear();
            if (this.level.hasNeighborSignal(this.worldPosition)) {
                this.activatedBlocks.add(this.worldPosition.immutable());
            }

            for (BlockPos part : door.structurePositions(this.worldPosition, this.getFacing(), this.getExtraHeight())) {
                if (this.level.hasNeighborSignal(part)) {
                    this.activatedBlocks.add(part.immutable());
                }
            }

            this.redstonePowered = !this.activatedBlocks.isEmpty();
        }
    }

    private void applyRedstoneState(boolean powered) {
        boolean changed = this.redstonePowered != powered;
        this.redstonePowered = powered;
        if (this.redstoneMode != DoorRedstoneMode.NONE && !this.redstoneMode.isGfbs() && changed) {
            switch (this.redstoneMode) {
                case PULSE:
                    if (powered) {
                        this.toggleFromRedstone();
                    }
                    break;
                case TRIGGER:
                    this.setOpenFromRedstone(powered);
                    break;
                case INVERTED:
                    this.setOpenFromRedstone(!powered);
            }
        }
    }

    private void applyModeTarget(boolean powered) {
        switch (this.redstoneMode) {
            case TRIGGER:
                this.setOpenFromRedstone(powered);
                break;
            case INVERTED:
                this.setOpenFromRedstone(!powered);
                break;
            case GFBS_NORMALLY_CLOSED:
                this.setOpenFromRedstone(false);
                break;
            case GFBS_NORMALLY_OPEN:
                this.setOpenFromRedstone(true);
        }
    }

    public void applyGateDesiredState(boolean open) {
        if (this.level != null && !this.level.isClientSide && this.core && this.redstoneMode.isGfbs()) {
            this.setOpenFromRedstone(open);
        }
    }

    public void commandSetOpen(boolean open) {
        if (this.level != null && !this.level.isClientSide && this.core) {
            if (this.isBlastDoor()) {
                this.setGroupOpenFromRedstone(open);
            } else {
                this.setOpenFromRedstone(open);
            }
        }
    }

    public void commandToggle() {
        if (this.core) {
            this.commandSetOpen(this.state != 1 && this.state != 3);
        }
    }

    private void toggleFromRedstone() {
        if (this.state == 0 || this.state == 1) {
            if (this.isBlastDoor()) {
                this.setGroupOpenFromRedstone(this.state == 0);
            } else {
                this.soundLeader = true;
                this.silentTransition = false;
                this.toggleInternal();
            }
        }
    }

    private boolean isBlastDoor() {
        return "blast_door".equals(this.getSpec().id);
    }

    private void setGroupOpenFromRedstone(boolean open) {
        for (DoorBlockEntity door : this.findBlastDoorGroup()) {
            boolean leader = door == this;
            door.soundLeader = leader;
            door.silentTransition = !leader;
            if (open && door.state == 0) {
                door.toggleInternal();
            } else if (!open && door.state == 1) {
                door.toggleInternal();
            }
        }
    }

    private void setOpenFromRedstone(boolean open) {
        if (this.state == 3 || this.state == 2) {
            boolean headingOpen = this.state == 3;
            this.pendingOpen = open == headingOpen ? null : open;
        } else if (this.isBlastDoor()) {
            this.setGroupOpenFromRedstone(open);
        } else if (open && this.state == 0) {
            this.toggleFromRedstone();
        } else if (!open && this.state == 1) {
            this.toggleFromRedstone();
        }
    }

    private void applyPendingOpen() {
        if (this.pendingOpen != null) {
            boolean open = this.pendingOpen;
            this.pendingOpen = null;
            if (this.isBlastDoor()) {
                this.setGroupOpenFromRedstone(open);
            } else if (open && this.state == 0) {
                this.toggleFromRedstone();
            } else if (!open && this.state == 1) {
                this.toggleFromRedstone();
            }
        }
    }

    public void tick() {
        if (this.level != null && this.core) {
            DoorSpec spec = this.getSpec();
            boolean justFinished = false;
            int duration = this.getTimeToOpen();
            if (this.state == 3) {
                if (this.openTicks < duration) {
                    this.openTicks++;
                }

                if (this.openTicks >= duration) {
                    this.openTicks = duration;
                    this.setState((byte)1);
                    justFinished = true;
                }
            } else if (this.state == 2) {
                if (this.openTicks > 0) {
                    this.openTicks--;
                }

                if (this.openTicks <= 0) {
                    this.openTicks = 0;
                    this.setState((byte)0);
                    justFinished = true;
                }
            }

            if (this.level.isClientSide) {
                DoorClientSoundHooks.tick(this);
            } else {
                if (spec.inheritsFrom("vault_door")) {
                    this.tickVaultSounds();
                }

                if (spec.inheritsFrom("reactor_door")) {
                    this.tickReactorSounds();
                }

                if (justFinished) {
                    this.applyPendingOpen();
                }

                this.tickRedstone();
            }
        }
    }

    private void tickVaultSounds() {
        if (this.state == 3) {
            if (this.openTicks >= 45 && this.openTicks <= 115 && (this.openTicks - 45) % 10 == 0) {
                this.playSound("block.vault_thud_new");
            }
        } else if (this.state == 2) {
            if (this.openTicks == 30) {
                this.playSound("block.vault_scrape_new");
            }

            if (this.openTicks >= 45 && this.openTicks <= 115 && (this.openTicks - 45) % 10 == 0) {
                this.playSound("block.vault_thud_new");
            }
        }
    }

    private void tickReactorSounds() {
        int duration = this.getTimeToOpen();
        if (this.state == 3) {
            if (this.openTicks == 1) {
                this.playSound("door.reactor_door_handle");
            }

            if (this.openTicks == 43) {
                this.playSound("door.reactor_door_open");
            }
        } else if (this.state == 2) {
            if (this.openTicks == duration - 23) {
                this.playSound("door.reactor_door_close");
            }

            if (this.openTicks == duration - 66) {
                this.playSound("door.reactor_door_handle");
            }
        }
    }

    private void tickRedstone() {
        this.applyRedstoneState(!this.activatedBlocks.isEmpty());
    }

    public void sync() {
        if (this.level != null && !this.level.isClientSide) {
            this.setChanged();
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
        }
    }

    public void onLoad() {
        if (this.core) {
            DoorCoreRegistry.register(this);
        }
    }

    public void setRemoved() {
        DoorCoreRegistry.unregister(this);
        if (this.level != null && this.level.isClientSide) {
            DoorClientSoundHooks.stop(this);
        }

        super.setRemoved();
    }

    public void onChunkUnloaded() {
        if (this.level != null && this.level.isClientSide) {
            DoorClientSoundHooks.stop(this);
        }

        super.onChunkUnloaded();
    }

    public void removeStructure() {
        if (this.level != null && !this.level.isClientSide) {
            Block block = this.getBlockState().getBlock();
            if (block instanceof DoorBlock door) {
                Direction facing = this.getFacing();

                for (BlockPos pos : door.structurePositions(this.worldPosition, facing, this.getExtraHeight())) {
                    if (this.level.getBlockState(pos).getBlock() == block) {
                        this.level.removeBlock(pos, false);
                    }
                }

                this.level.removeBlock(this.worldPosition, false);
            }
        }
    }

    public AABB getRenderBoundingBox() {
        DoorSpec spec = this.getSpec();
        if ("transition_seal".equals(spec.id)) {
            return new AABB(this.worldPosition).inflate(64.0);
        } else {
            int extent = 1;

            for (int d : spec.dimensions) {
                extent = Math.max(extent, d);
            }

            for (int[] extra : spec.extraDimensions) {
                for (int d : extra) {
                    extent = Math.max(extent, d);
                }
            }

            if ("blast_door".equals(spec.id)) {
                extent = Math.max(extent, spec.dimensions[0] + this.getExtraHeight());
            }

            return new AABB(this.worldPosition).inflate((double)(extent + 8));
        }
    }

    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("core", this.core);
        tag.putByte("state", this.state);
        tag.putInt("openTicks", this.openTicks);
        tag.putByte("skin", this.skin);
        tag.putByte("redstoneMode", (byte)this.redstoneMode.ordinal());
        tag.putBoolean("manualControl", this.manualControl);
        tag.putBoolean("redstonePowered", this.redstonePowered);
        tag.putByte("pendingOpen", (byte)(this.pendingOpen == null ? -1 : (this.pendingOpen ? 1 : 0)));
        tag.putBoolean("soundLeader", this.soundLeader);
        tag.putInt("extraHeight", this.extraHeight);
        tag.putInt("localX", this.localX);
        tag.putInt("localY", this.localY);
        tag.putInt("localZ", this.localZ);
        if (this.corePos != null) {
            tag.putInt("coreX", this.corePos.getX());
            tag.putInt("coreY", this.corePos.getY());
            tag.putInt("coreZ", this.corePos.getZ());
        }

        ListTag powered = new ListTag();

        for (BlockPos pos : this.activatedBlocks) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("x", pos.getX());
            entry.putInt("y", pos.getY());
            entry.putInt("z", pos.getZ());
            powered.add(entry);
        }

        tag.put("powered", powered);
    }

    public void load(CompoundTag tag) {
        super.load(tag);
        this.core = tag.contains("core")
            ? tag.getBoolean("core")
            : this.getBlockState().getBlock() instanceof DoorBlock && (Boolean)this.getBlockState().getValue(DoorBlock.CORE);
        this.state = tag.getByte("state");
        this.openTicks = tag.getInt("openTicks");
        this.skin = tag.getByte("skin");
        this.redstoneMode = tag.contains("redstoneMode") ? DoorRedstoneMode.byOrdinal(tag.getByte("redstoneMode")) : DoorRedstoneMode.TRIGGER;
        this.manualControl = !tag.contains("manualControl") || tag.getBoolean("manualControl");
        this.redstonePowered = tag.getBoolean("redstonePowered");
        if (tag.contains("pendingOpen")) {
            byte pending = tag.getByte("pendingOpen");
            this.pendingOpen = pending < 0 ? null : pending > 0;
        } else {
            this.pendingOpen = null;
        }

        this.soundLeader = !tag.contains("soundLeader") || tag.getBoolean("soundLeader");
        this.extraHeight = clampExtraHeight(tag.getInt("extraHeight"));
        this.localX = tag.getInt("localX");
        this.localY = tag.getInt("localY");
        this.localZ = tag.getInt("localZ");
        if (tag.contains("coreX")) {
            this.corePos = new BlockPos(tag.getInt("coreX"), tag.getInt("coreY"), tag.getInt("coreZ"));
        } else {
            this.corePos = this.worldPosition;
        }

        this.activatedBlocks.clear();
        ListTag powered = tag.getList("powered", 10);

        for (int i = 0; i < powered.size(); i++) {
            CompoundTag entry = powered.getCompound(i);
            this.activatedBlocks.add(new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z")));
        }
    }

    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) {
            byte old = this.state;
            this.load(packet.getTag());
            if (this.level != null && this.level.isClientSide && old != this.state) {
                DoorClientSoundHooks.onStateChanged(this, old, this.state);
            }
        }
    }
}
