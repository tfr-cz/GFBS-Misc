package org.lytharalab.gfbs.doors.content.armor;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * HBM's {@code HBM_T51} armour material.
 *
 * <p>NTM builds it as {@code EnumHelper.addArmorMaterial("HBM_T51", 150, new int[]{3, 8, 6, 3}, 0)}:
 * {@code maxDamage} 150, defence 3/8/6/3 (helmet, chest, legs, boots), enchantability 0. In 1.7.10 the
 * max damage is multiplied per slot by {11, 16, 15, 13}, which is where the durability numbers below come
 * from. Toughness and knockback resistance do not exist in 1.7.10 and stay at zero.</p>
 */
public final class PowerArmorMaterial implements ArmorMaterial {
    public static final PowerArmorMaterial INSTANCE = new PowerArmorMaterial();

    /** 150 x {11, 16, 15, 13}, in the slot order the API asks for. */
    private static final int HELMET_DURABILITY = 150 * 11;
    private static final int CHEST_DURABILITY = 150 * 16;
    private static final int LEGS_DURABILITY = 150 * 15;
    private static final int BOOTS_DURABILITY = 150 * 13;

    private PowerArmorMaterial() {
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> HELMET_DURABILITY;
            case CHESTPLATE -> CHEST_DURABILITY;
            case LEGGINGS -> LEGS_DURABILITY;
            case BOOTS -> BOOTS_DURABILITY;
        };
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 3;
            case CHESTPLATE -> 8;
            case LEGGINGS -> 6;
            case BOOTS -> 3;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_IRON;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.IRON_INGOT);
    }

    @Override
    public String getName() {
        return "gfbs_power_armor";
    }

    @Override
    public float getToughness() {
        return 0.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}
