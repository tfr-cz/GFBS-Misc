package org.lytharalab.gfbs.doors.content.armor;

import net.minecraft.world.item.ArmorItem;
import org.junit.jupiter.api.Test;
import org.lytharalab.gfbs.doors.content.armor.resistance.ArmorResistance;
import org.lytharalab.gfbs.doors.content.armor.resistance.DamageCategory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the Euphemium set against the numbers HBM actually ships.
 *
 * <p>They come from {@code MainRegistry.aMatEuph} and {@code DamageResistanceHandler}, both of which are easy to
 * get wrong: the armour values look like the T-51b's, and the resistance entry reads as "1000000 damage
 * reduction" until you notice {@code dr} is the blocked fraction, not the damage.</p>
 */
class EuphemiumArmorTest {
    private static final float EPSILON = 1.0E-4F;

    @Test
    void theMaterialMatchesHbm() {
        EuphemiumArmorMaterial material = EuphemiumArmorMaterial.INSTANCE;
        assertEquals(3, material.getDefenseForType(ArmorItem.Type.HELMET));
        assertEquals(8, material.getDefenseForType(ArmorItem.Type.CHESTPLATE));
        assertEquals(6, material.getDefenseForType(ArmorItem.Type.LEGGINGS));
        assertEquals(3, material.getDefenseForType(ArmorItem.Type.BOOTS));
        assertEquals(100, material.getEnchantmentValue(), "addArmorMaterial's fourth argument");
    }

    @Test
    void durabilityIsTheHbmBaseTimesTheVanillaSlotSplit() {
        EuphemiumArmorMaterial material = EuphemiumArmorMaterial.INSTANCE;
        assertEquals(15_000_000 * 11, material.getDurabilityForType(ArmorItem.Type.HELMET));
        assertEquals(15_000_000 * 16, material.getDurabilityForType(ArmorItem.Type.CHESTPLATE));
        assertEquals(15_000_000 * 15, material.getDurabilityForType(ArmorItem.Type.LEGGINGS));
        assertEquals(15_000_000 * 13, material.getDurabilityForType(ArmorItem.Type.BOOTS));
    }

    @Test
    void theFullSetNullifiesAnyHitBelowAMillion() {
        ArmorResistance entry = EuphemiumArmorMaterial.resistanceTable().resolveCategory(DamageCategory.OTHER);
        assertTrue(entry.isComplete(), "dr 1.0 means nothing gets through");
        assertEquals(0.0F, entry.apply(20.0F), EPSILON);
        assertEquals(0.0F, entry.apply(999_999.0F), EPSILON);
        assertEquals(0.0F, entry.apply(1_000_000.0F), EPSILON, "the threshold itself is still absorbed");
    }

    @Test
    void evenAHitAboveTheThresholdIsStillFullyResisted() {
        // Nothing in 1.20.1 deals a million, but the entry has to keep behaving: (1.5M - 1M) * (1 - 1) = 0.
        ArmorResistance entry = EuphemiumArmorMaterial.resistanceTable().resolveCategory(DamageCategory.OTHER);
        assertEquals(0.0F, entry.apply(1_500_000.0F), EPSILON);
    }
}
