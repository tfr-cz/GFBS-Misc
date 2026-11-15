package org.lytharalab.gfbs.doors.content.armor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.world.damagesource.DamageTypes;
import org.lytharalab.gfbs.doors.client.armor.EmptyArmorModel;
import org.lytharalab.gfbs.doors.content.armor.resistance.ArmorResistanceTable;
import org.lytharalab.gfbs.doors.content.armor.resistance.ArmorResistances;
import org.lytharalab.gfbs.doors.content.armor.resistance.DamageCategory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * One piece of HBM's T-51b power armour - appearance only, no power, no battery, no HUD.
 *
 * <p>These pieces are not drawn by the vanilla armour layer: {@code PowerArmorLayer} renders the
 * imported OBJ instead, because that layer can only draw a {@link HumanoidModel} made of boxes and the
 * imported mesh is far too detailed for that. Handing the layer an empty humanoid keeps it from
 * drawing anything at all, so there is no double image and no vanilla armour texture in the way.</p>
 */
public final class PowerArmorItem extends ArmorItem {
    /**
     * HBM's {@code registerSet} entry for the T-51b. One item differs from NTM: HBM's exact entry used an
     * obfuscated 1.7.10 damage source that cannot be resolved offline; fall damage is the one that makes
     * sense for a suit whose landing slam fires at 10 blocks, so that is what the immunity is attached to.
     */
    public static void registerResistances(java.util.function.Supplier<Item> helmet, java.util.function.Supplier<Item> chest,
                                           java.util.function.Supplier<Item> legs, java.util.function.Supplier<Item> boots) {
        ArmorResistances.registerSet(helmet.get(), chest.get(), legs.get(), boots.get(), ArmorResistanceTable.builder()
            .addCategory(DamageCategory.PHYSICAL, 2.0F, 0.15F)
            .addCategory(DamageCategory.FIRE, 0.5F, 0.35F)
            .addCategory(DamageCategory.EXPLOSION, 5.0F, 0.25F)
            .addExact(DamageTypes.FALL, 0.0F, 1.0F)
            .setOther(0.0F, 0.1F)
            .build());
    }

    public PowerArmorItem(ArmorItem.Type type, Properties properties) {
        super(PowerArmorMaterial.INSTANCE, type, properties);
    }

    /**
     * Never drawn: the vanilla layer renders {@code EmptyArmorModel} for these pieces. It still resolves a
     * texture though, so point it at one that exists instead of letting it ask for a
     * {@code textures/models/armor/<material>_layer_1.png} sheet that was never shipped.
     */
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, @Nullable String type) {
        return "gfbs_doors:textures/armor/hazmat_chest.png";
    }

    /**
     * HBM's {@code hasFSBArmor}: every armour slot holding this suit. Shared by the set bonus, the
     * impact behaviour and the footstep trigger so they can never disagree about what "wearing it" means.
     */
    public static boolean isCompleteSet(LivingEntity entity) {
        for (EquipmentSlot slot : new EquipmentSlot[]{
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof PowerArmorItem)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The durability bar is repurposed as the charge gauge, exactly like HBM: hidden while full, and it
     * shows how much is left from the charge ratio rather than from damage. 1.20.1 asks for the bar in
     * three parts ({@code isBarVisible} / {@code getBarWidth} / {@code getBarColor}) where 1.7.10 had
     * {@code showDurabilityBar} / {@code getDurabilityForDisplay}.
     */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return PowerArmorCharge.get(stack) < PowerArmorCharge.MAX_CHARGE;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * (float) PowerArmorCharge.get(stack) / (float) PowerArmorCharge.MAX_CHARGE);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return PowerArmorCharge.barColor((float) PowerArmorCharge.get(stack) / (float) PowerArmorCharge.MAX_CHARGE);
    }

    /**
     * HBM's {@code ArmorFSBPowered#setDamage}: the suit never wears out, incoming durability damage is paid
     * in charge instead ({@code amount * consumption}). Every armour damage path funnels through here, so
     * one override covers both combat and any other source of durability loss.
     */
    @Override
    public void setDamage(ItemStack stack, int damage) {
        // hurt() hands over the *accumulated* damage (current + amount), not the increment: the item never stores
        // damage because this override does not call super, so the stored value stays where it started and only
        // the difference is this hit's worth. Charging the absolute value would bill a fresh hit for the whole
        // history - which is exactly how a suit lost hundreds of thousands of charge to one scratch.
        int current = stack.getDamageValue();
        int increment = damage - current;
        if (increment > 0) {
            PowerArmorCharge.discharge(stack, (long) increment * PowerArmorCharge.DAMAGE_CONSUMPTION);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Charge: " + PowerArmorCharge.shortNumber(PowerArmorCharge.get(stack))
            + " / " + PowerArmorCharge.shortNumber(PowerArmorCharge.MAX_CHARGE)).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                          EquipmentSlot equipmentSlot,
                                                          HumanoidModel<?> original) {
                return EmptyArmorModel.get();
            }
        });
    }
}
