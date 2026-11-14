package org.lytharalab.gfbs.doors.client.armor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the bar geometry copied from HBM's {@code onHUDRenderBar}: an 81px track with a 1px inset and a 1px
 * fill line, so the numbers here are the ones a player sees change as the suit drains.
 */
class PowerArmorHudTest {
    private static final int LEFT = 100;

    @Test
    void anEmptyBarLeavesOnlyTheInset() {
        assertEquals(LEFT + 1, PowerArmorHud.fillEnd(LEFT, 0.0F));
    }

    @Test
    void aFullBarStopsOnePixelShortOfTheTrackEnd() {
        assertEquals(LEFT + 80, PowerArmorHud.fillEnd(LEFT, 1.0F));
    }

    @Test
    void halfChargeFillsHalfTheWidth() {
        assertEquals(LEFT + 41, PowerArmorHud.fillEnd(LEFT, 0.5F), "1 + round(79 * 0.5)");
    }

    @Test
    void theFillColourFollowsTheCharge() {
        assertEquals(0x00FF00, PowerArmorHud.fillColor(1.0F));
        assertEquals(0xFFFF00, PowerArmorHud.fillColor(0.5F));
        assertEquals(0xFF0000, PowerArmorHud.fillColor(0.0F));
    }

    @Test
    void outOfRangeRatiosClampToTheTrack() {
        assertEquals(LEFT + 1, PowerArmorHud.fillEnd(LEFT, -2.0F));
        assertEquals(LEFT + 80, PowerArmorHud.fillEnd(LEFT, 4.0F));
    }
}
