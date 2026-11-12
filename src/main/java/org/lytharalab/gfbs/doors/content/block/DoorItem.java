package org.lytharalab.gfbs.doors.content.block;

import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.lytharalab.gfbs.doors.client.DoorItemRenderer;
import org.lytharalab.gfbs.doors.content.DoorSpec;

public class DoorItem extends BlockItem {
    private final DoorSpec spec;

    public DoorItem(Block block, Properties properties, DoorSpec spec) {
        super(block, properties);
        this.spec = spec;
    }

    public DoorSpec spec() {
        return this.spec;
    }

    public void initializeClient(Consumer consumer) {
        consumer.accept(new IClientItemExtensions() {
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return DoorItemRenderer.getInstance();
            }
        });
    }

    public String getDescriptionId() {
        return this.spec.itemDescriptionId();
    }

    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (context.getPlayer() == null) {
            return InteractionResult.PASS;
        } else if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            BlockPlaceContext placeContext = new BlockPlaceContext(context);
            BlockPos target = context.getClickedPos();
            if (!level.getBlockState(target).canBeReplaced(placeContext)) {
                target = target.relative(context.getClickedFace());
            }

            int i = Mth.floor((double)(context.getPlayer().getYRot() * 4.0F / 360.0F) + 0.5) & 3;

            Direction facing = switch (i) {
                case 0 -> Direction.NORTH;
                case 1 -> Direction.EAST;
                case 2 -> Direction.SOUTH;
                default -> Direction.WEST;
            };
            BlockPos core = target.relative(facing, -this.spec.offset);
            Block block = this.getBlock();
            if (block instanceof DoorBlock door && DoorBlock.canPlaceAt(level, core, facing, door, target, placeContext)) {
                BlockState coreState = (BlockState)((BlockState)block.defaultBlockState().setValue(DoorBlock.FACING, facing)).setValue(DoorBlock.CORE, true);
                level.setBlock(core, coreState, 3);
                if (level.getBlockEntity(core) instanceof DoorBlockEntity be) {
                    be.initCore();
                }

                level.sendBlockUpdated(core, coreState, coreState, 2);

                for (BlockPos part : door.structurePositions(core, facing)) {
                    BlockState partState = (BlockState)((BlockState)block.defaultBlockState().setValue(DoorBlock.FACING, facing)).setValue(DoorBlock.CORE, false);
                    level.setBlock(part, partState, 3);
                    if (level.getBlockEntity(part) instanceof DoorBlockEntity be) {
                        be.initPart(core, part.getX() - core.getX(), part.getY() - core.getY(), part.getZ() - core.getZ());
                    }

                    level.sendBlockUpdated(part, partState, partState, 2);
                }

                SoundType sound = coreState.getSoundType();
                level.playSound(null, core, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
                if (level.getBlockEntity(core) instanceof DoorBlockEntity coreBe) {
                    coreBe.scanRedstone();
                    coreBe.syncGroupOnJoin();
                }

                if (!context.getPlayer().getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }

                return InteractionResult.sidedSuccess(false);
            }

            return InteractionResult.FAIL;
        }
    }

    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (this.spec.skinCount() > 1) {
            tooltip.add(Component.translatable("tooltip.gfbs_doors.change_skin").withStyle(ChatFormatting.GRAY));
        }

        tooltip.add(Component.translatable("tooltip.gfbs_doors.redstone").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.gfbs_doors.configurator_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
