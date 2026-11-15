package org.lytharalab.gfbs.doors.content.weapon;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * HBM's {@code CRUCIBLE} tool material: {@code EnumHelper.addToolMaterial("CRUCIBLE", 10, 3, 50.0F, 100.0F, 0)}.
 *
 * <p>Nothing here except the attack damage is visible in 1.20.1 - harvest level, mining speed and durability are
 * 1.7.10 concepts. The durability of 3 is not a typo either: NTM used the sword's three damage points as its three
 * charges. That is the part this port drops on purpose (the sword runs on infinite energy), so
 * {@link CrucibleItem#canBeDepleted()} turns the whole durability path off.</p>
 */
public final class CrucibleTier implements Tier {
    public static final CrucibleTier INSTANCE = new CrucibleTier();

    static final int LEVEL = 10;
    static final int USES = 3;
    static final float SPEED = 50.0F;
    /** The material's damage bonus; HBM's real number is the {@code 5000.0F} passed to {@code ItemCrucible}. */
    static final float ATTACK_DAMAGE_BONUS = 100.0F;
    static final int ENCHANTMENT_VALUE = 0;

    private CrucibleTier() {
    }

    @Override
    public int getUses() {
        return USES;
    }

    @Override
    public float getSpeed() {
        return SPEED;
    }

    @Override
    public float getAttackDamageBonus() {
        return ATTACK_DAMAGE_BONUS;
    }

    @Override
    public int getLevel() {
        return LEVEL;
    }

    @Override
    public int getEnchantmentValue() {
        return ENCHANTMENT_VALUE;
    }

    /** 1.7.10's addToolMaterial takes a repair material name no item matches; nothing repairs it. */
    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }
}
