package org.lytharalab.gfbs.doors.content.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
import org.lytharalab.gfbs.doors.content.DoorSpec;

import javax.annotation.Nullable;
import java.util.List;

public class DoorItem extends BlockItem {
    private final DoorSpec spec;

    public DoorItem(Block block, Item.Properties properties, DoorSpec spec) {
        super(block, properties);
        this.spec = spec;
    }

    public DoorSpec spec() {
        return spec;
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
        return spec.itemDescriptionId();
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Level level = context.getLevel();
        if (context.getPlayer() == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        BlockPos target = context.getClickedPos();
        if (!level.getBlockState(target).canBeReplaced(placeContext)) {
            target = target.relative(context.getClickedFace());
        }

        int i = Mth.floor(context.getPlayer().getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        Direction facing = switch (i) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            default -> Direction.WEST;
        };

        BlockPos core = target.relative(facing, -spec.offset);
        Block block = getBlock();
        if (!(block instanceof DoorBlock door) || !DoorBlock.canPlaceAt(level, core, facing, door, target, placeContext)) {
            return InteractionResult.FAIL;
        }

        BlockState coreState = block.defaultBlockState()
                .setValue(DoorBlock.FACING, facing)
                .setValue(DoorBlock.CORE, true);
        level.setBlock(core, coreState, Block.UPDATE_ALL);
        if (level.getBlockEntity(core) instanceof DoorBlockEntity be) {
            be.initCore();
        }
        level.sendBlockUpdated(core, coreState, coreState, Block.UPDATE_CLIENTS);

        for (BlockPos part : door.structurePositions(core, facing)) {
            BlockState partState = block.defaultBlockState()
                    .setValue(DoorBlock.FACING, facing)
                    .setValue(DoorBlock.CORE, false);
            level.setBlock(part, partState, Block.UPDATE_ALL);
            if (level.getBlockEntity(part) instanceof DoorBlockEntity be) {
                be.initPart(core, part.getX() - core.getX(), part.getY() - core.getY(), part.getZ() - core.getZ());
            }
            level.sendBlockUpdated(part, partState, partState, Block.UPDATE_CLIENTS);
        }

        SoundType sound = coreState.getSoundType();
        level.playSound(null, core, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1F) / 2F, sound.getPitch() * 0.8F);

        if (level.getBlockEntity(core) instanceof DoorBlockEntity coreBe) {
            coreBe.scanRedstone();
            coreBe.syncGroupOnJoin();
        }

        if (!context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (spec.skinCount() > 1) {
            tooltip.add(Component.translatable("tooltip.gfbs_doors.change_skin").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.gfbs_doors.redstone").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.gfbs_doors.configurator_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
