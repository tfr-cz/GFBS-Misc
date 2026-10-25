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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.lytharalab.cabinelevator.content.ModItems;
import org.lytharalab.cabinelevator.data.DimPos;
import org.lytharalab.cabinelevator.data.ElevatorSavedData;
import org.lytharalab.cabinelevator.elevator.ElevatorManager;
import org.lytharalab.cabinelevator.elevator.ElevatorService;

import java.util.List;

public class ElevatorControllerBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public ElevatorControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElevatorControllerBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level instanceof ServerLevel serverLevel) {
            ElevatorSavedData.get(serverLevel.getServer()).getOrCreateController(DimPos.of(serverLevel, pos));
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level instanceof ServerLevel serverLevel && state.getBlock() != newState.getBlock()) {
            DimPos key = DimPos.of(serverLevel, pos);
            ElevatorSavedData.get(serverLevel.getServer()).removeController(key);
            ElevatorManager.get(serverLevel.getServer()).onControllerRemoved(key);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
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
            ElevatorSavedData.get(serverLevel.getServer()).getOrCreateController(DimPos.of(serverLevel, pos));
            ElevatorService.openController(serverPlayer, serverLevel, pos);
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
        tooltip.add(Component.translatable("tooltip.cabin_elevator.controller.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.cabin_elevator.controller.2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
