package org.lytharalab.gfbs.doors.content.armor.resistance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pins HBM's formula: absorb the threshold, then scale what is left by {@code 1 - dr}. */
class ArmorResistanceTest {
    private static final float EPSILON = 1.0E-4F;

    @Test
    void thresholdIsAbsorbedBeforeThePercentage() {
        ArmorResistance phys = new ArmorResistance(2.0F, 0.15F);
        assertEquals(6.8F, phys.apply(10.0F), EPSILON, "(10 - 2) * 0.85");
    }

    @Test
    void hitsAtOrBelowTheThresholdDoNothing() {
        ArmorResistance phys = new ArmorResistance(2.0F, 0.15F);
        assertEquals(0.0F, phys.apply(2.0F), EPSILON);
        assertEquals(0.0F, phys.apply(1.5F), EPSILON);
    }

    @Test
    void fullResistanceIsImmunity() {
        ArmorResistance fall = new ArmorResistance(0.0F, 1.0F);
        assertTrue(fall.isComplete());
        assertEquals(0.0F, fall.apply(100.0F), EPSILON);
        assertFalse(new ArmorResistance(0.0F, 0.25F).isComplete());
    }

    @Test
    void theT51bExplosionEntryMatchesHbm() {
        ArmorResistance explosion = new ArmorResistance(5.0F, 0.25F);
        assertEquals(3.75F, explosion.apply(10.0F), EPSILON, "(10 - 5) * 0.75");
        assertEquals(0.0F, explosion.apply(4.0F), EPSILON);
    }
}
