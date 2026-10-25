package org.lytharalab.cabinelevator.content.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.lytharalab.cabinelevator.data.DimPos;
import org.lytharalab.cabinelevator.data.ElevatorSavedData;
import org.lytharalab.cabinelevator.elevator.ElevatorManager;
import org.lytharalab.cabinelevator.elevator.ElevatorService;

import java.util.List;

public class ElevatorCallerBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public ElevatorCallerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level instanceof ServerLevel serverLevel) {
            ElevatorSavedData.get(serverLevel.getServer()).getOrCreateCaller(DimPos.of(serverLevel, pos));
            if (level.hasNeighborSignal(pos) && !state.getValue(POWERED)) {
                level.setBlock(pos, state.setValue(POWERED, Boolean.TRUE), 3);
            }
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                 BlockPos neighborPos, boolean isMoving) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) {
            return;
        }
        level.setBlock(pos, state.setValue(POWERED, powered), 3);
        if (powered) {
            ElevatorManager.get(serverLevel.getServer()).requestCall(null, DimPos.of(serverLevel, pos));
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level instanceof ServerLevel serverLevel && state.getBlock() != newState.getBlock()) {
            ElevatorSavedData.get(serverLevel.getServer()).removeCaller(DimPos.of(serverLevel, pos));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown()) {
                ElevatorService.showCallerStatus(serverPlayer, serverLevel, pos);
            } else {
                ElevatorManager.get(serverLevel.getServer()).requestCall(serverPlayer, DimPos.of(serverLevel, pos));
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.cabin_elevator.caller.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.cabin_elevator.caller.2").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.cabin_elevator.caller.3").withStyle(ChatFormatting.DARK_GRAY));
    }
}
