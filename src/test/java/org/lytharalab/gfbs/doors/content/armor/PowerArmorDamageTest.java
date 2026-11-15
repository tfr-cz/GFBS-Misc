package org.lytharalab.gfbs.doors.content.armor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins how a damage event becomes a charge bill.
 *
 * <p>{@code ItemStack.hurt} calls {@code setDamage} with {@code stored + amount}, so the bill has to subtract what
 * the stack still carries. Older builds left a value there; treating the incoming number as the increment is what
 * billed a whole suit's history to one scratch.</p>
 */
class PowerArmorDamageTest {
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
