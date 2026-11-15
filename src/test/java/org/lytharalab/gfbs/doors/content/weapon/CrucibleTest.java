package org.lytharalab.gfbs.doors.content.weapon;

import org.junit.jupiter.api.Test;
import org.lytharalab.gfbs.doors.TestBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pins the numbers HBM's Crucible ships with, and the one thing this port changes about it.
 *
 * <p>The 5000 is not a bonus: NTM passed it as the whole attack damage, which is why the modifier has to subtract
 * both the player's bare-handed point and the tool material's own 100.</p>
 */
class CrucibleTest {
    @Test
    void theToolMaterialMatchesHbm() {
        assertEquals(3, CrucibleTier.INSTANCE.getUses(), "the three charges NTM counted hits with");
        assertEquals(50.0F, CrucibleTier.INSTANCE.getSpeed());
        assertEquals(100.0F, CrucibleTier.INSTANCE.getAttackDamageBonus());
        assertEquals(10, CrucibleTier.INSTANCE.getLevel());
        assertEquals(0, CrucibleTier.INSTANCE.getEnchantmentValue());
    }

    @Test
    void theSwordStillDealsFiveThousand() {
        assertEquals(5000, CrucibleItem.totalAttackDamage());
    }

    @Test
    void infiniteEnergyMeansItCannotBeDepleted() {
        assumeTrue(TestBootstrap.itemsAvailable(), "Forge bootstrap unavailable in this JVM");
        assertFalse(new CrucibleItem().canBeDepleted(), "no durability, so no charge is ever spent");
    }
}
