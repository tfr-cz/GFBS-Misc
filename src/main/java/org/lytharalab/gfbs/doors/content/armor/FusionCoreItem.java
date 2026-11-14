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
 * HBM's {@code fusion_core}, the item that recharges powered armour: {@code ItemFusionCore} in NTM.
 *
 * <p>NTM registers it as {@code new ItemFusionCore(2_500_000)} - one core pushes 2.5M HE into every worn
 * battery piece (each capped at its own max), needs the full set worn, is consumed, and is not stackable.
 * All four of those are kept here, so one core is a full recharge of a T-51b set and nothing more.</p>
 *
 * <p>Only the worn pieces are charged; armour sitting in the inventory keeps whatever it had.</p>
 */
public final class FusionCoreItem extends Item {
    /** The amount HBM's item is constructed with; per worn piece, before the per-piece cap clamps it. */
    public static final long CHARGE_PER_PIECE = 2_500_000L;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public FusionCoreItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack core = player.getItemInHand(hand);
        if (!PowerArmorCharge.hasFullSet(player)) {
            return InteractionResultHolder.fail(core);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(core);
        }

        boolean charged = false;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (PowerArmorCharge.add(player.getItemBySlot(slot), CHARGE_PER_PIECE) > 0L) {
                charged = true;
            }
        }
        if (!charged) {
            return InteractionResultHolder.fail(core);
        }

        if (!player.getAbilities().instabuild) {
            core.shrink(1);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.5F);
        return InteractionResultHolder.success(core);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gfbs_doors.fusion_core",
            PowerArmorCharge.shortNumber(CHARGE_PER_PIECE)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.gfbs_doors.fusion_core.requires").withStyle(ChatFormatting.GRAY));
    }
}
