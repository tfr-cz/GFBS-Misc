package org.lytharalab.gfbs.doors.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.lytharalab.gfbs.doors.content.ModBlockEntities;
import org.lytharalab.gfbs.doors.content.ModItems;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class CargoElevatorBlock extends Block implements EntityBlock {
    public static final BooleanProperty CORE = BooleanProperty.create("core");

    public CargoElevatorBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5F, 50F)
                .sound(SoundType.METAL)
                .noOcclusion()
                .dynamicShape()
                .requiresCorrectToolForDrops()
                .isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false));
        registerDefaultState(stateDefinition.any().setValue(CORE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CORE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CargoElevatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.CARGO_ELEVATOR.get()) return null;
        return (tickLevel, tickPos, tickState, blockEntity) -> {
            if (blockEntity instanceof CargoElevatorBlockEntity elevator) elevator.tick();
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CargoElevatorBlockEntity be)) return InteractionResult.PASS;
        CargoElevatorBlockEntity core = be.getCore();
        if (core == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack held = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() && held.is(ModItems.CARGO_ELEVATOR_ITEM.get())) {
            return core.tryExtend(player) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        core.toggle();
        return InteractionResult.CONSUME;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof CargoElevatorBlockEntity be) {
            CargoElevatorBlockEntity core = be.getCore();
            if (core != null) return core.getPartShape(pos);
        }
        // An unresolved elevator must not become a solid cube (that blocks the shaft and
        // hides missing block entity data); stay passable until the core is found.
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1F;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CargoElevatorBlockEntity be) {
            CargoElevatorBlockEntity core = be.getCore();
            if (core != null) {
                if (!player.isCreative()) {
                    int count = core.getHeight() + 1;
                    while (count > 0) {
                        int stack = Math.min(count, 64);
                        Block.popResource(level, core.getBlockPos(), new ItemStack(ModItems.CARGO_ELEVATOR_ITEM.get(), stack));
                        count -= stack;
                    }
                }
                core.removeStructure();
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        return new ItemStack(ModItems.CARGO_ELEVATOR_ITEM.get());
    }

    public static VoxelShape elevatorShape(int dx, int dy, int dz, double extension) {
        VoxelShape shape = Shapes.empty();
        // Platform standing surface follows the extension continuously so vanilla collision
        // carries riders (players and other entities) instead of teleporting them.
        double surface = 1D + extension;
        int floor = (int) Math.floor(surface);
        double frac = surface - floor;
        if (frac > 0.0001D) {
            if (dy == floor) {
                shape = Shapes.or(shape, Shapes.box(0, 0, 0, 1, Math.min(1D, frac), 1));
            }
        } else if (dy == floor - 1) {
            shape = Shapes.or(shape, Shapes.box(0, 0.9375, 0, 1, 1, 1));
        }
        if (Math.abs(dx) == 1 && Math.abs(dz) == 1) {
            double x0 = dx < 0 ? 0 : 0.75;
            double z0 = dz < 0 ? 0 : 0.75;
            shape = Shapes.or(shape, Shapes.box(x0, 0, z0, x0 + 0.25, 1, z0 + 0.25));
        }
        return shape;
    }
}
