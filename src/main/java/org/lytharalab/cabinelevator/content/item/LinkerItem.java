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
import org.lytharalab.cabinelevator.content.block.ElevatorCallerBlock;
import org.lytharalab.cabinelevator.content.block.ElevatorControllerBlock;
import org.lytharalab.cabinelevator.content.block.RedstoneInterfaceBlock;
import org.lytharalab.cabinelevator.data.DimPos;
import org.lytharalab.cabinelevator.data.ElevatorSavedData;
import org.lytharalab.cabinelevator.elevator.ElevatorService;
import org.lytharalab.cabinelevator.util.Msg;

import java.util.List;

public class LinkerItem extends Item {
    private static final String TAG_CONTROLLER = "Controller";

    public LinkerItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static DimPos getBinding(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_CONTROLLER, Tag.TAG_COMPOUND)) {
            return null;
        }
        return DimPos.load(tag.getCompound(TAG_CONTROLLER));
    }

    private static void setBinding(ItemStack stack, DimPos controller) {
        stack.getOrCreateTag().put(TAG_CONTROLLER, controller.save());
    }

    private static void clearBinding(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(TAG_CONTROLLER);
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
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        boolean controller = state.getBlock() instanceof ElevatorControllerBlock;
        boolean redstoneInterface = state.getBlock() instanceof RedstoneInterfaceBlock;
        boolean caller = state.getBlock() instanceof ElevatorCallerBlock;
        if (!controller && !redstoneInterface && !caller) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            if (controller && !player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        DimPos clicked = DimPos.of(serverLevel, pos);
        if (controller) {
            if (!player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            ElevatorSavedData.ControllerEntry entry = ElevatorSavedData.get(serverLevel.getServer())
                    .getOrCreateController(clicked);
            setBinding(stack, clicked);
            syncItem(serverPlayer, context.getHand(), stack);
            Object channel = entry.channel.isBlank()
                    ? Component.translatable("gui.cabin_elevator.not_set")
                    : Component.literal(entry.channel);
            Msg.success(serverPlayer, "message.cabin_elevator.linker.controller_saved",
                    channel, pos.getX(), pos.getY(), pos.getZ());
            return InteractionResult.SUCCESS;
        }

        DimPos controllerPos = getBinding(stack);
        if (controllerPos == null) {
            Msg.error(serverPlayer, "message.cabin_elevator.linker.need_controller");
            return InteractionResult.SUCCESS;
        }
        if (!controllerPos.dimension().equals(serverLevel.dimension())) {
            Msg.error(serverPlayer, "message.cabin_elevator.dim_mismatch");
            return InteractionResult.SUCCESS;
        }

        if (redstoneInterface) {
            ElevatorService.bindInterface(serverPlayer, serverLevel, pos, controllerPos);
        } else {
            ElevatorService.bindCaller(serverPlayer, serverLevel, pos, controllerPos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || getBinding(stack) == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            clearBinding(stack);
            syncItem(serverPlayer, hand, stack);
            Msg.success(serverPlayer, "message.cabin_elevator.linker.cleared");
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getBinding(stack) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        DimPos binding = getBinding(stack);
        if (binding != null) {
            tooltip.add(Component.translatable("tooltip.cabin_elevator.linker.bound",
                            binding.dimension().location().toString(),
                            binding.pos().getX(), binding.pos().getY(), binding.pos().getZ())
                    .withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("tooltip.cabin_elevator.linker.empty").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.cabin_elevator.linker.1").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.cabin_elevator.linker.2").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.cabin_elevator.linker.3").withStyle(ChatFormatting.DARK_GRAY));
    }
}
