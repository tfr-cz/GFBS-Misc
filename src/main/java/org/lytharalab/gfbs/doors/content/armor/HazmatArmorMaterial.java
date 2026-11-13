package org.lytharalab.gfbs.doors.content.armor;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * The protective suit's armor material.
 *
 * <p>Modelled on HBM's power-armor sets: a coarse four-piece outfit with hand-written models and
 * per-slot textures rather than the vanilla 64x32 armor sheet (see {@link HazmatArmorItem}).</p>
 */
public final class HazmatArmorMaterial implements ArmorMaterial {
    public static final HazmatArmorMaterial INSTANCE = new HazmatArmorMaterial();

    /** Same durability split vanilla uses: base x {11, 16, 15, 13}. */
    private static final int BASE_DURABILITY = 20;

    private HazmatArmorMaterial() {
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 11 * BASE_DURABILITY;
            case CHESTPLATE -> 16 * BASE_DURABILITY;
            case LEGGINGS -> 15 * BASE_DURABILITY;
            case BOOTS -> 13 * BASE_DURABILITY;
            default -> 11 * BASE_DURABILITY;
        };
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 2;
            case CHESTPLATE -> 7;
            case LEGGINGS -> 5;
            case BOOTS -> 2;
            default -> 0;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 12;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.LEATHER);
    }

    @Override
    public String getName() {
        return "gfbs_doors:hazmat";
    }

    @Override
    public float getToughness() {
        return 0.5F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}
