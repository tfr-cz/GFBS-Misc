package org.lytharalab.gfbs.doors.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.lytharalab.gfbs.doors.content.CargoElevatorRiders;
import org.lytharalab.gfbs.doors.content.ModBlockEntities;
import org.lytharalab.gfbs.doors.content.ModItems;
import org.lytharalab.gfbs.misc.network.MiscNetwork;

import javax.annotation.Nullable;
import java.util.List;

public class CargoElevatorBlockEntity extends BlockEntity {
    private static final double SPEED = 2D / 20D;
    private static final int MAX_HEIGHT = 64;

    private boolean core;
    @Nullable
    private BlockPos corePos;
    private transient long nextCoreLookupTick;
    private transient int coreLookupAttempts;


    private transient long nextHeightProbeTick;
    private transient int heightProbeAttempts;
    private int localDY;

    private int height;
    private double extension;
    private double prevExtension;
    private int targetExtension;
    private double syncExtension;
    private int sync;
    private double lastSyncedExtension = Double.NaN;
    private int lastSyncedTarget;

    public CargoElevatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CARGO_ELEVATOR.get(), pos, state);
        this.core = state.getValue(CargoElevatorBlock.CORE);
        this.corePos = pos;
    }

    public boolean isCore() {
        return core;
    }

    public void initCore() {
        this.core = true;
        this.corePos = worldPosition;
        this.height = 0;
        this.extension = 0;
        this.prevExtension = 0;
        this.targetExtension = 0;
        this.syncExtension = 0;
        this.lastSyncedExtension = Double.NaN;
        this.lastSyncedTarget = 0;
        setChanged();
    }

    public void initPart(BlockPos corePos, int dy) {
        this.core = false;
        this.corePos = corePos.immutable();
        this.localDY = dy;
        setChanged();
    }

    @Nullable
    public CargoElevatorBlockEntity getCore() {
        if (core) return this;
        if (level == null) return null;
        if (corePos != null) {
            BlockEntity be = level.getBlockEntity(corePos);
            if (be instanceof CargoElevatorBlockEntity elevator && elevator.core) return elevator;
        }
        long now = level.getGameTime();
        if (now >= nextCoreLookupTick && coreLookupAttempts < 120) {
            coreLookupAttempts++;
            nextCoreLookupTick = now + 20;
            CargoElevatorBlockEntity found = findCoreNearby();
            if (found != null) {
                this.corePos = found.worldPosition.immutable();
                setChanged();
                return found;
            }
        }
        return null;
    }

    // Self-healing for parts whose stored core position is missing or stale (pasted
    // structures, old saves): the core always sits in the same 3x3 footprint, so scan
    // downward through the column once and cache the result.
    @Nullable
    private CargoElevatorBlockEntity findCoreNearby() {
        if (level == null) return null;
        int minY = level.getMinBuildHeight();
        for (int down = 0; down <= MAX_HEIGHT + 1; down++) {
            BlockPos base = worldPosition.below(down);
            if (base.getY() < minY) break;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos candidate = base.offset(dx, 0, dz);
                    if (level.getBlockEntity(candidate) instanceof CargoElevatorBlockEntity elevator
                            && elevator.core) {
                        return elevator;
                    }
                }
            }
        }
        return null;
    }

    public int getTargetExtension() {
        return targetExtension;
    }

    public int getHeight() {
        return height;
    }

    public double getExtension() {
        return extension;
    }

    public double getPrevExtension() {
        return prevExtension;
    }

    public double getExtensionInterp(float partialTick) {
        return prevExtension + (extension - prevExtension) * partialTick;
    }

    public VoxelShape getPartShape(BlockPos part) {
        int dx = part.getX() - worldPosition.getX();
        int dy = part.getY() - worldPosition.getY();
        int dz = part.getZ() - worldPosition.getZ();
        // Lag the collision surface by one tick: the slab grows between the rider's physics
        // steps, and an entity starting inside a collision shape falls straight through it.
        return CargoElevatorBlock.elevatorShape(dx, dy, dz, Math.min(extension, prevExtension));
    }

    public void toggle() {
        if (level == null || level.isClientSide) return;
        targetExtension = targetExtension == 0 ? height : 0;
        setChanged();
        sync();
    }

    public boolean tryExtend(Player player) {
        if (level == null || level.isClientSide) return false;
        if (height >= MAX_HEIGHT) return false;

        BlockPos layer = worldPosition.above(height + 1);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = layer.offset(dx, 0, dz);
                if (!level.getBlockState(pos).canBeReplaced()) return false;
            }
        }

        Block block = getBlockState().getBlock();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = layer.offset(dx, 0, dz);
                BlockState state = block.defaultBlockState().setValue(CargoElevatorBlock.CORE, false);
                level.setBlock(pos, state, Block.UPDATE_ALL);
                if (level.getBlockEntity(pos) instanceof CargoElevatorBlockEntity be) {
                    be.initPart(worldPosition, height + 1);
                }
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }

        height++;
        setChanged();
        sync();

        ItemStack held = player.getMainHandItem();
        if (!player.getAbilities().instabuild) held.shrink(1);
        return true;
    }

    public void tick() {
        if (level == null || !core) return;

        prevExtension = extension;

        if (!level.isClientSide) {
            if (extension < targetExtension) {
                extension = Math.min(extension + SPEED, targetExtension);
            } else if (extension > targetExtension) {
                extension = Math.max(extension - SPEED, targetExtension);
            }
            extension = Mth.clamp(extension, 0, height);

            if (height == 0 && heightProbeAttempts < 120 && level.getGameTime() >= nextHeightProbeTick) {
                heightProbeAttempts++;
                nextHeightProbeTick = level.getGameTime() + 20;
                int inferred = inferHeight();
                if (inferred > 0) {
                    height = inferred;
                    setChanged();
                    sync();
                }
            }

            if (extension != lastSyncedExtension || targetExtension != lastSyncedTarget) {
                lastSyncedExtension = extension;
                lastSyncedTarget = targetExtension;
                if (level instanceof ServerLevel serverLevel) {
                    MiscNetwork.sendCargoSync(serverLevel, worldPosition, extension);
                }
            }
        } else {
            if (sync > 0) {
                extension += (syncExtension - extension) / sync;
                sync--;
            } else {
                extension = syncExtension;
            }
        }
    }

    public void carryRiders() {
        if (level == null || level.isClientSide || !core) return;
        double platformY = worldPosition.getY() + 1 + extension;
        double window = 0.75D;
        AABB area = new AABB(worldPosition.getX() - 0.99, platformY - window, worldPosition.getZ() - 0.99,
                worldPosition.getX() + 1.99, platformY + window, worldPosition.getZ() + 1.99);
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, area);
        for (Entity entity : entities) {
            if (entity instanceof Player || entity.isSpectator()) continue;
            double feet = entity.getY();
            if (feet < platformY - window || feet > platformY + window) continue;
            double delta = platformY - feet;
            if (Math.abs(delta) > 1.0E-4D) {
                entity.setPos(entity.getX(), entity.getY() + delta, entity.getZ());
            }
            entity.setDeltaMovement(entity.getDeltaMovement().x, 0.0D, entity.getDeltaMovement().z);
            entity.setOnGround(true);
            entity.resetFallDistance();
        }
    }

    public void sync() {
        if (level == null || level.isClientSide) return;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public void removeStructure() {
        if (level == null || level.isClientSide) return;
        Block block = getBlockState().getBlock();
        for (int dy = 0; dy <= height; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = worldPosition.offset(dx, dy, dz);
                    if (level.getBlockState(pos).getBlock() == block) {
                        level.removeBlock(pos, false);
                    }
                }
            }
        }
        level.removeBlock(worldPosition, false);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, height + 2, 2));
    }

    @Override
    public void onLoad() {
        if (core) {
            CargoElevatorRiders.register(this);
        }
    }

    @Override
    public void setRemoved() {
        CargoElevatorRiders.unregister(this);
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("core", core);
        tag.putInt("localDY", localDY);
        tag.putInt("height", height);
        tag.putDouble("extension", extension);
        tag.putInt("targetExtension", targetExtension);
        if (corePos != null) {
            tag.putInt("coreX", corePos.getX());
            tag.putInt("coreY", corePos.getY());
            tag.putInt("coreZ", corePos.getZ());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        core = tag.getBoolean("core");
        localDY = tag.getInt("localDY");
        height = tag.getInt("height");
        extension = tag.getDouble("extension");
        syncExtension = extension;
        targetExtension = tag.getInt("targetExtension");
        lastSyncedExtension = Double.NaN;
        lastSyncedTarget = targetExtension;
        if (tag.contains("coreX")) {
            corePos = new BlockPos(tag.getInt("coreX"), tag.getInt("coreY"), tag.getInt("coreZ"));
        } else {
            corePos = worldPosition;
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

    private int inferHeight() {
        if (level == null) {
            return 0;
        }
        Block block = getBlockState().getBlock();
        int inferred = 0;
        while (inferred < MAX_HEIGHT) {
            int layer = inferred + 1;
            boolean full = true;
            for (int dx = -1; dx <= 1 && full; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (level.getBlockState(worldPosition.offset(dx, layer, dz)).getBlock() != block) {
                        full = false;
                        break;
                    }
                }
            }
            if (!full) {
                break;
            }
            inferred = layer;
        }
        return inferred;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() == null || level == null) return;
        CompoundTag tag = packet.getTag();
        core = tag.getBoolean("core");
        height = tag.getInt("height");
        targetExtension = tag.getInt("targetExtension");
        applyClientSync(tag.getDouble("extension"));
    }

    public void applyClientSync(double newExtension) {
        syncExtension = newExtension;
        sync = newExtension != extension ? 3 : 0;
    }
}
