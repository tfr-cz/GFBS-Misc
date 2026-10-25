package org.lytharalab.cabinelevator.content.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.lytharalab.cabinelevator.content.ModItems;
import org.lytharalab.cabinelevator.data.DimPos;
import org.lytharalab.cabinelevator.data.ElevatorSavedData;
import org.lytharalab.cabinelevator.elevator.ElevatorService;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class RedstoneInterfaceBlock extends Block {
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    private static final Map<Direction, BooleanProperty> FACE_PROPERTIES = new EnumMap<>(Direction.class);

    static {
        FACE_PROPERTIES.put(Direction.NORTH, NORTH);
        FACE_PROPERTIES.put(Direction.EAST, EAST);
        FACE_PROPERTIES.put(Direction.SOUTH, SOUTH);
        FACE_PROPERTIES.put(Direction.WEST, WEST);
        FACE_PROPERTIES.put(Direction.UP, UP);
        FACE_PROPERTIES.put(Direction.DOWN, DOWN);
    }

    public RedstoneInterfaceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWER, 0)
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER, NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getNearestLookingDirection().getOpposite();
        return defaultBlockState().setValue(property(face), true);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level instanceof ServerLevel serverLevel) {
            ElevatorSavedData.get(serverLevel.getServer())
                    .getOrCreateInterface(DimPos.of(serverLevel, pos), maskFromState(state));
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level instanceof ServerLevel serverLevel && state.getBlock() != newState.getBlock()) {
            ElevatorSavedData.get(serverLevel.getServer()).removeInterface(DimPos.of(serverLevel, pos));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        // Consumers query with the direction from themselves toward this block
        // (SignalGetter.hasNeighborSignal passes D for the neighbor at pos.relative(D)),
        // so a consumer on face F queries with F.getOpposite().
        BooleanProperty face = FACE_PROPERTIES.get(side.getOpposite());
        return face != null && state.getValue(face) ? state.getValue(POWER) : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return getSignal(state, level, pos, side);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWER) != 0) {
            level.setBlock(pos, state.setValue(POWER, 0), 3);
            notifyOutputs(level, pos, state);
        }
    }

    public static void notifyOutputs(ServerLevel level, BlockPos pos, BlockState state) {
        // Strongly charged consumers (lamp/dust behind a solid block on an output face) never
        // hear about POWER changes through setBlock alone, so push updates two hops like
        // vanilla DiodeBlock.updateNeighborsInFront does.
        for (Map.Entry<Direction, BooleanProperty> entry : FACE_PROPERTIES.entrySet()) {
            if (state.getValue(entry.getValue())) {
                level.updateNeighborsAt(pos.relative(entry.getKey()), state.getBlock());
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty() && !stack.is(ModItems.LINKER.get())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            ElevatorSavedData.get(serverLevel.getServer())
                    .getOrCreateInterface(DimPos.of(serverLevel, pos), maskFromState(state));
            ElevatorService.openInterface(serverPlayer, serverLevel, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    public static BooleanProperty property(Direction direction) {
        return FACE_PROPERTIES.get(direction);
    }

    public static int maskFromState(BlockState state) {
        int mask = 0;
        for (Map.Entry<Direction, BooleanProperty> entry : FACE_PROPERTIES.entrySet()) {
            if (state.getValue(entry.getValue())) {
                mask |= 1 << entry.getKey().get3DDataValue();
            }
        }
        return mask;
    }

    public static boolean hasFace(int mask, Direction direction) {
        return (mask & 1 << direction.get3DDataValue()) != 0;
    }

    public static BlockState applyFaces(BlockState state, int mask) {
        BlockState result = state;
        for (Map.Entry<Direction, BooleanProperty> entry : FACE_PROPERTIES.entrySet()) {
            result = result.setValue(entry.getValue(), hasFace(mask, entry.getKey()));
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.cabin_elevator.interface.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.cabin_elevator.interface.2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
