package org.lytharalab.gfbs.doors.content.armor;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lytharalab.gfbs.doors.TestBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins how a damage event becomes a charge bill.
 *
 * <p>{@code ItemStack.hurt} calls {@code setDamage} with {@code stored + amount}, so the bill has to subtract what
 * the stack still carries. Older builds left a value there; treating the incoming number as the increment is what
 * billed a whole suit's history to one scratch.</p>
 *
 * <p>The bootstrap below is not about items: reaching {@code PowerArmorItem} at all loads {@code ArmorItem} and
 * then {@code Item}, which refuses to initialise before the vanilla registries exist. Asking for it here keeps the
 * class independent of whichever test JUnit happens to run first.</p>
 */
class PowerArmorDamageTest {
    @BeforeAll
    static void bootstrapRegistries() {
        TestBootstrap.itemsAvailable();
    }

    @Test
    void aFreshPiecePaysForTheWholeHit() {
        assertEquals(5, PowerArmorItem.damageCost(5, 0));
    }

    @Test
    void aPieceWithStoredDamageOnlyPaysTheDifference() {
        assertEquals(5, PowerArmorItem.damageCost(36, 31), "36 stored+hit, 31 already stored");
    }

    @Test
    void aStoredValueLargerThanTheHitCostsNothing() {
        assertEquals(0, PowerArmorItem.damageCost(3, 31));
    }
}
