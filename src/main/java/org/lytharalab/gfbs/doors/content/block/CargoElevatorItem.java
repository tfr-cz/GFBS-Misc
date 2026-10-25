package org.lytharalab.gfbs.doors.content.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.lytharalab.gfbs.doors.content.ModItems;

import javax.annotation.Nullable;
import java.util.List;

public class CargoElevatorItem extends BlockItem {
    public CargoElevatorItem(Block block, Item.Properties properties) {
        super(block, properties);
    }


    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return org.lytharalab.gfbs.doors.client.DoorItemRenderer.getInstance();
            }
        });
    }

    @Override
    public String getDescriptionId() {
        return "item.gfbs_doors.cargo_elevator";
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Level level = context.getLevel();
        if (context.getPlayer() == null) return InteractionResult.PASS;
        if (level.getBlockState(context.getClickedPos()).getBlock() instanceof CargoElevatorBlock) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        BlockPos target = context.getClickedPos();
        if (!level.getBlockState(target).canBeReplaced(placeContext)) {
            target = target.relative(context.getClickedFace());
        }
        if (level.isOutsideBuildHeight(target) || level.isOutsideBuildHeight(target.above())) {
            return InteractionResult.FAIL;
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = target.offset(dx, 0, dz);
                if (!level.getBlockState(pos).canBeReplaced()) return InteractionResult.FAIL;
            }
        }

        Block block = getBlock();
        BlockState coreState = block.defaultBlockState().setValue(CargoElevatorBlock.CORE, true);
        level.setBlock(target, coreState, Block.UPDATE_ALL);
        if (level.getBlockEntity(target) instanceof CargoElevatorBlockEntity be) {
            be.initCore();
        }
        level.sendBlockUpdated(target, coreState, coreState, Block.UPDATE_CLIENTS);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos pos = target.offset(dx, 0, dz);
                BlockState state = block.defaultBlockState().setValue(CargoElevatorBlock.CORE, false);
                level.setBlock(pos, state, Block.UPDATE_ALL);
                if (level.getBlockEntity(pos) instanceof CargoElevatorBlockEntity be) {
                    be.initPart(target, 0);
                }
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }

        SoundType sound = coreState.getSoundType();
        level.playSound(null, target, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1F) / 2F, sound.getPitch() * 0.8F);

        if (!context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gfbs_doors.elevator").withStyle(ChatFormatting.GRAY));
    }
}
