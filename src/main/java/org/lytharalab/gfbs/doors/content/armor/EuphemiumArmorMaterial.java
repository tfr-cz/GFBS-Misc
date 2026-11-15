package org.lytharalab.gfbs.doors.content.armor;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import org.lytharalab.gfbs.doors.content.armor.resistance.ArmorResistanceTable;

/**
 * HBM's {@code HBM_EUPHEMIUM} armour material.
 *
 * <p>NTM builds it as {@code EnumHelper.addArmorMaterial("HBM_EUPHEMIUM", 15000000, new int[]{3, 8, 6, 3}, 100)}:
 * {@code maxDamage} 15,000,000, defence 3/8/6/3 (helmet, chestplate, leggings, boots), enchantability 100. In
 * 1.7.10 the max damage is multiplied per slot by {11, 16, 15, 13}, which is where the durability numbers below
 * come from. Those numbers are decoration: {@link EuphemiumArmorItem#setDamage} never stores anything, exactly
 * like NTM's comment "do literally nothing lole".</p>
 */
public final class EuphemiumArmorMaterial implements ArmorMaterial {
    public static final EuphemiumArmorMaterial INSTANCE = new EuphemiumArmorMaterial();

    /** 15,000,000 x {11, 16, 15, 13}, in the slot order the API asks for. */
    private static final int BASE_DURABILITY = 15_000_000;
    private static final int HELMET_DURABILITY = BASE_DURABILITY * 11;
    private static final int CHEST_DURABILITY = BASE_DURABILITY * 16;
    private static final int LEGS_DURABILITY = BASE_DURABILITY * 15;
    private static final int BOOTS_DURABILITY = BASE_DURABILITY * 13;
    /** The amount a single hit may not exceed to be nullified outright. */
    private static final float NULLIFY_THRESHOLD = 1_000_000.0F;

    private EuphemiumArmorMaterial() {
    }

    /**
     * The full-set damage profile: absorb up to a million per hit and block everything else
     * (HBM's {@code DamageResistanceHandler: setOther(1000000.0F, 1.0F)}).
     *
     * <p>It sits with the material rather than on the item because it describes the outfit, not a slot - and a
     * material loads without the item registry, so the numbers stay unit-testable.</p>
     */
    static ArmorResistanceTable resistanceTable() {
        return ArmorResistanceTable.builder()
            .setOther(NULLIFY_THRESHOLD, 1.0F)
            .build();
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
        return 100;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_GENERIC;
    }

    /**
     * Nothing repairs it: NTM passes {@code EnumHelper.addArmorMaterial} a repair material name that no item
     * ever matches, which is the 1.7.10 way of saying "none".
     */
    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }

    @Override
    public String getName() {
        return "gfbs_doors:euphemium";
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
