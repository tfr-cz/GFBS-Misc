package org.lytharalab.cabinelevator.content.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.lytharalab.cabinelevator.content.block.ElevatorControllerBlock;
import org.lytharalab.cabinelevator.data.DimPos;
import org.lytharalab.cabinelevator.elevator.ElevatorService;
import org.lytharalab.cabinelevator.util.Msg;

import java.util.List;

public class RangeSelectorItem extends Item {
    private static final String TAG_CONTROLLER = "Controller";
    private static final String TAG_CORNER = "Corner";

    public RangeSelectorItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static DimPos getController(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_CONTROLLER, Tag.TAG_COMPOUND)) {
            return null;
        }
        return DimPos.load(tag.getCompound(TAG_CONTROLLER));
    }

    @Nullable
    public static DimPos getCorner(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_CORNER, Tag.TAG_COMPOUND)) {
            return null;
        }
        return DimPos.load(tag.getCompound(TAG_CORNER));
    }

    private static void setController(ItemStack stack, DimPos controller) {
        stack.getOrCreateTag().put(TAG_CONTROLLER, controller.save());
    }

    private static void setCorner(ItemStack stack, DimPos corner) {
        stack.getOrCreateTag().put(TAG_CORNER, corner.save());
    }

    private static void clearCorner(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(TAG_CORNER);
        }
    }

    private static void clearAll(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(TAG_CONTROLLER);
            tag.remove(TAG_CORNER);
        }
    }

    private static void syncItem(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        player.setItemInHand(hand, stack);
        if (player.containerMenu != null) {
            player.containerMenu.broadcastChanges();
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        if (player.isShiftKeyDown()) {
            BlockState state = level.getBlockState(clickedPos);
            if (state.getBlock() instanceof ElevatorControllerBlock) {
                DimPos controller = DimPos.of(serverLevel, clickedPos);
                setController(stack, controller);
                clearCorner(stack);
                syncItem(serverPlayer, context.getHand(), stack);
                Msg.success(serverPlayer, "message.cabin_elevator.range.controller_selected",
                        clickedPos.getX(), clickedPos.getY(), clickedPos.getZ());
            } else {
                Msg.error(serverPlayer, "message.cabin_elevator.range.select_controller");
            }
            return InteractionResult.SUCCESS;
        }

        DimPos corner = getCorner(stack);
        DimPos clicked = DimPos.of(serverLevel, clickedPos);
        if (corner == null || !corner.dimension().equals(serverLevel.dimension())) {
            setCorner(stack, clicked);
            syncItem(serverPlayer, context.getHand(), stack);
            Msg.success(serverPlayer, "message.cabin_elevator.range.corner_first",
                    clickedPos.getX(), clickedPos.getY(), clickedPos.getZ());
            return InteractionResult.SUCCESS;
        }

        DimPos controller = getController(stack);
        if (ElevatorService.applyRange(serverPlayer, serverLevel, controller, corner.pos(), clickedPos)) {
            clearCorner(stack);
            syncItem(serverPlayer, context.getHand(), stack);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            clearAll(stack);
            syncItem(serverPlayer, hand, stack);
            Msg.success(serverPlayer, "message.cabin_elevator.range.cleared");
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getController(stack) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        DimPos controller = getController(stack);
        DimPos corner = getCorner(stack);
        if (controller != null) {
            tooltip.add(Component.translatable("tooltip.cabin_elevator.range_selector.controller",
                            controller.pos().getX(), controller.pos().getY(), controller.pos().getZ())
                    .withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("tooltip.cabin_elevator.range_selector.no_controller")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (corner != null) {
            tooltip.add(Component.translatable("tooltip.cabin_elevator.range_selector.corner",
                            corner.pos().getX(), corner.pos().getY(), corner.pos().getZ())
                    .withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip.cabin_elevator.range_selector.1")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.cabin_elevator.range_selector.2")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.cabin_elevator.range_selector.3")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
