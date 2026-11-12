package org.lytharalab.gfbs.doors.content.block;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.lytharalab.gfbs.doors.content.DoorConfigService;
import org.lytharalab.gfbs.doors.content.DoorGeometry;
import org.lytharalab.gfbs.doors.content.DoorSpec;
import org.lytharalab.gfbs.doors.content.ModBlockEntities;
import org.lytharalab.gfbs.doors.content.ModItems;

public class DoorBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty CORE = BooleanProperty.create("core");
    private final DoorSpec spec;

    public DoorBlock(DoorSpec spec) {
        super(
            Properties.of()
                .mapColor(MapColor.METAL)
                .strength(spec.hardness, spec.resistance)
                .sound(SoundType.METAL)
                .noOcclusion()
                .dynamicShape()
                .requiresCorrectToolForDrops()
                .isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false)
        );
        this.spec = spec;
        this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(CORE, false));
    }

    public DoorSpec spec() {
        return this.spec;
    }

    protected void createBlockStateDefinition(Builder builder) {
        builder.add(new Property[]{FACING, CORE});
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DoorBlockEntity(pos, state);
    }

    @Nullable
    public BlockEntityTicker getTicker(Level level, BlockState state, BlockEntityType type) {
        return type != ModBlockEntities.DOOR.get() ? null : (tickLevel, tickPos, tickState, blockEntity) -> {
            if (blockEntity instanceof DoorBlockEntity door) {
                door.tick();
            }
        };
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof DoorBlockEntity be) {
            DoorBlockEntity core = be.getCore();
            if (core == null) {
                return InteractionResult.PASS;
            } else {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.is((Item)ModItems.DOOR_CONFIGURATOR.get())) {
                    if (level.isClientSide) {
                        return InteractionResult.SUCCESS;
                    } else {
                        if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
                            DoorConfigService.open(serverPlayer, serverLevel, pos);
                        }

                        return InteractionResult.CONSUME;
                    }
                } else if (player.isShiftKeyDown()) {
                    if (!player.isCreative()) {
                        return InteractionResult.PASS;
                    } else if (core.getSpec().skinCount() > 1) {
                        if (!level.isClientSide) {
                            core.cycleSkin();
                        }

                        return InteractionResult.sidedSuccess(level.isClientSide);
                    } else {
                        return InteractionResult.PASS;
                    }
                } else if (!core.isManualControl() || core.getRedstoneMode().isGfbs()) {
                    return InteractionResult.PASS;
                } else if (level.isClientSide) {
                    return InteractionResult.SUCCESS;
                } else if ("blast_door".equals(core.getSpec().id)) {
                    return core.toggleBlastDoorGroup(player) ? InteractionResult.CONSUME : InteractionResult.PASS;
                } else {
                    return core.toggle(player) ? InteractionResult.CONSUME : InteractionResult.PASS;
                }
            }
        } else {
            return InteractionResult.PASS;
        }
    }

    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DoorBlockEntity be) {
            DoorBlockEntity core = be.getCore();
            if (core != null) {
                core.onNeighborChanged(pos);
            }
        }

        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
    }

    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.doorShape(state, level, pos, false);
    }

    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.doorShape(state, level, pos, false);
    }

    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.doorShape(state, level, pos, true);
    }

    private VoxelShape doorShape(BlockState state, BlockGetter level, BlockPos pos, boolean collision) {
        if (!(level.getBlockEntity(pos) instanceof DoorBlockEntity be)) {
            return Shapes.block();
        } else {
            DoorBlockEntity core = be.getCore();
            if (core == null) {
                return Shapes.block();
            } else {
                AABB box = DoorGeometry.worldBox(
                    this.spec, core.getBlockPos(), pos, (Direction)state.getValue(FACING), core.isOpenForCollision(), collision, core.getExtraHeight()
                );
                if (!DoorGeometry.isEmpty(box)) {
                    return Shapes.create(box.move((double)(-pos.getX()), (double)(-pos.getY()), (double)(-pos.getZ())));
                } else {
                    return !collision && pos.equals(core.getBlockPos())
                        ? Shapes.create(new AABB(0.4375, 0.4375, 0.4375, 0.5625, 0.5625, 0.5625))
                        : Shapes.empty();
                }
            }
        }
    }

    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        if (!(level.getBlockEntity(pos) instanceof DoorBlockEntity be)) {
            return false;
        } else {
            DoorBlockEntity core = be.getCore();
            return core != null && core.isOpenForCollision();
        }
    }

    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DoorBlockEntity be) {
            DoorBlockEntity core = be.getCore();
            if (core != null) {
                if (!player.isCreative()) {
                    Block.popResource(level, core.getBlockPos(), new ItemStack((ItemLike)ModItems.door(this.spec.id).get()));
                }

                core.removeStructure();
            }
        }

        super.playerWillDestroy(level, pos, state, player);
    }

    public List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        return Collections.emptyList();
    }

    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        return new ItemStack((ItemLike)ModItems.door(this.spec.id).get());
    }

    public Set<BlockPos> structurePositions(BlockPos core, Direction facing) {
        return this.structurePositions(core, facing, 0);
    }

    public Set<BlockPos> structurePositions(BlockPos core, Direction facing, int extraHeight) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        int[] dimensions = this.spec.dimensions;
        int extra = "blast_door".equals(this.spec.id) ? Math.max(0, extraHeight) : 0;
        if (extra > 0) {
            dimensions = new int[]{dimensions[0] + extra, dimensions[1], dimensions[2], dimensions[3], dimensions[4], dimensions[5]};
        }

        addDimension(positions, core, facing, dimensions);

        for (int[] dimension : this.spec.extraDimensions) {
            addDimension(positions, core, facing, dimension);
        }

        positions.remove(core);
        return positions;
    }

    private static void addDimension(Set<BlockPos> positions, BlockPos core, Direction facing, int[] dimensions) {
        int[] rot = DoorGeometry.rotateDims(dimensions, facing);

        for (int dy = -rot[1]; dy <= rot[0]; dy++) {
            for (int dx = -rot[4]; dx <= rot[5]; dx++) {
                for (int dz = -rot[2]; dz <= rot[3]; dz++) {
                    positions.add(core.offset(dx, dy, dz));
                }
            }
        }
    }

    public static boolean canPlaceAt(Level level, BlockPos core, Direction facing, DoorBlock block, BlockPos target, BlockPlaceContext context) {
        if (level.isOutsideBuildHeight(core)) {
            return false;
        } else {
            Set<BlockPos> positions = block.structurePositions(core, facing);
            positions.add(core);
            if (!positions.contains(target)) {
                return false;
            } else if (positions.size() > 2000) {
                return false;
            } else {
                for (BlockPos pos : positions) {
                    if (level.isOutsideBuildHeight(pos)) {
                        return false;
                    }

                    BlockState existing = level.getBlockState(pos);
                    if (pos.equals(target)) {
                        if (!existing.canBeReplaced(context)) {
                            return false;
                        }
                    } else if (!existing.canBeReplaced()) {
                        return false;
                    }
                }

                return true;
            }
        }
    }
}
