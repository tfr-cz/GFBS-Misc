package org.lytharalab.gfbs.doors.content.armor;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import org.lytharalab.gfbs.doors.TestBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pins the charge model copied from HBM's {@code ArmorFSBPowered}.
 *
 * <p>The saturation and drain maths run against pure helpers, because a bare test JVM cannot finish
 * Forge's bootstrap and therefore cannot build an {@code ItemStack}; the one round-trip test that needs a
 * stack is skipped there instead of pretending to pass.</p>
 */
class PowerArmorChargeTest {
    @Test
    void chargeIsClampedIntoRange() {
        assertEquals(0L, PowerArmorCharge.clampCharge(-50L));
        assertEquals(PowerArmorCharge.MAX_CHARGE, PowerArmorCharge.clampCharge(PowerArmorCharge.MAX_CHARGE * 3));
        assertEquals(1_234L, PowerArmorCharge.clampCharge(1_234L));
    }

    @Test
    void dischargeStopsAtZero() {
        assertEquals(0L, PowerArmorCharge.removed(12L, 100L));
        assertEquals(88L, PowerArmorCharge.removed(100L, 12L));
        assertEquals(0L, PowerArmorCharge.removed(0L, 5L), "already empty");
    }

    @Test
    void addStopsAtMax() {
        assertEquals(PowerArmorCharge.MAX_CHARGE, PowerArmorCharge.added(PowerArmorCharge.MAX_CHARGE - 1_000L, 500_000L));
        assertEquals(1_000L, PowerArmorCharge.added(1_000L, -5L), "negative input adds nothing");
    }

    @Test
    void theSetLastsFiftyThousandTicks() {
        // The number the ticket pinned: four pieces paying 5/tick each.
        long perTick = PowerArmorCharge.DRAIN_PER_TICK * 4L;
        assertEquals(20L, perTick);
        assertEquals(50_000L, PowerArmorCharge.MAX_CHARGE / perTick);
    }

    @Test
    void combatCostsOnePercentPerTenDamage() {
        long afterOneHit = PowerArmorCharge.removed(PowerArmorCharge.MAX_CHARGE, PowerArmorCharge.DAMAGE_CONSUMPTION * 10L);
        assertEquals(PowerArmorCharge.MAX_CHARGE - 10_000L, afterOneHit);
        assertEquals(0.01F, (float) (PowerArmorCharge.MAX_CHARGE - afterOneHit) / (float) PowerArmorCharge.MAX_CHARGE, 1.0E-6F);
    }

    @Test
    void tooltipNumbersUseHbmSiSuffixes() {
        assertEquals("0", PowerArmorCharge.shortNumber(0L));
        assertEquals("999", PowerArmorCharge.shortNumber(999L));
        assertEquals("1.0k", PowerArmorCharge.shortNumber(1_000L));
        assertEquals("1.5k", PowerArmorCharge.shortNumber(1_500L));
        assertEquals("1.0M", PowerArmorCharge.shortNumber(1_000_000L));
        assertEquals("2.5M", PowerArmorCharge.shortNumber(2_500_000L));
    }

    @Test
    void theBarRampsGreenYellowRed() {
        // Vanilla's durability-bar hue, applied to charge instead of damage.
        assertEquals(0x00FF00, PowerArmorCharge.barColor(1.0F), "full is green");
        assertEquals(0xFFFF00, PowerArmorCharge.barColor(0.5F), "half is yellow");
        assertEquals(0xFF0000, PowerArmorCharge.barColor(0.0F), "empty is red");
        assertEquals(0xFF0000, PowerArmorCharge.barColor(-1.0F), "clamped low");
        assertEquals(0x00FF00, PowerArmorCharge.barColor(2.0F), "clamped high");
    }

    @Test
    void anItemStackRoundTripsThroughNbt() {
        assumeTrue(TestBootstrap.itemsAvailable(), "Forge bootstrap unavailable in this JVM");
        ItemStack stack = new ItemStack(new PowerArmorItem(ArmorItem.Type.HELMET, new net.minecraft.world.item.Item.Properties()));
        assertEquals(PowerArmorCharge.MAX_CHARGE, PowerArmorCharge.get(stack), "untagged reads as full");

        PowerArmorCharge.set(stack, 12L);
        assertEquals(12L, PowerArmorCharge.get(stack));
        assertEquals(12L, PowerArmorCharge.discharge(stack, 100L));
        assertEquals(0L, PowerArmorCharge.get(stack));
    }
}
