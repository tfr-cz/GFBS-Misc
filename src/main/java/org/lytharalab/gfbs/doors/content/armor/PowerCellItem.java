package org.lytharalab.gfbs.doors.content.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A one-shot cell that tops up the T-51b set, the stand-in for HBM's {@code armor_battery}.
 *
 * <p>One cell puts {@link PowerArmorCharge#MAX_CHARGE} into <em>each</em> worn piece - a full set, not a
 * single piece - and is consumed. That is a deliberate departure from NTM, where a battery charged one
 * item and a set needed four of them; this mod ships a single cell item, so four charges per click keeps
 * the loop to one action.</p>
 *
 * <p>Only the pieces being worn are charged. Armour sitting in the inventory keeps whatever it had.</p>
 */
public final class PowerCellItem extends Item {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public PowerCellItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack cell = player.getItemInHand(hand);
        if (!PowerArmorCharge.hasFullSet(player)) {
            return InteractionResultHolder.fail(cell);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(cell);
        }

        boolean charged = false;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (PowerArmorCharge.add(player.getItemBySlot(slot), PowerArmorCharge.MAX_CHARGE) > 0L) {
                charged = true;
            }
        }
        if (!charged) {
            return InteractionResultHolder.fail(cell);
        }

        if (!player.getAbilities().instabuild) {
            cell.shrink(1);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.5F);
        return InteractionResultHolder.success(cell);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gfbs_doors.power_cell").withStyle(ChatFormatting.GRAY));
    }
}
