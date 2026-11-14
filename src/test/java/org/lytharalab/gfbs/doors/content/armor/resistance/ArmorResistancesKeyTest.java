package org.lytharalab.gfbs.doors.content.armor.resistance;

import net.minecraft.world.item.Item;
import org.junit.jupiter.api.Test;
import org.lytharalab.gfbs.doors.TestBootstrap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Regression test for the crash where a zombie hitting the player threw
 * {@code NullPointerException: Ticking entity} out of {@code ArmorResistances.wornSetTable}.
 *
 * <p>The key used to be a {@code List.of(...)}: that factory rejects null elements, and an immutable
 * list throws from {@code contains(null)} - and empty armour slots are the normal case.</p>
 */
class ArmorResistancesKeyTest {
    private static boolean itemsUsable;

    @Test
    void emptySlotsMustNotThrow() {
        ArmorResistances.WornSet none = assertDoesNotThrow(
            () -> ArmorResistances.keyOf(null, null, null, null));
        assertEquals(none, ArmorResistances.keyOf(null, null, null, null));
    }

    @Test
    void partialSetsAreValidKeys() {
        assumeTrue(TestBootstrap.itemsAvailable(), "item registry unavailable in this environment");
        Item helmet = new Item(new Item.Properties());
        Item chest = new Item(new Item.Properties());
        ArmorResistances.WornSet first = assertDoesNotThrow(
            () -> ArmorResistances.keyOf(helmet, null, chest, null));
        assertEquals(first, ArmorResistances.keyOf(helmet, null, chest, null));
        assertNotEquals(first, ArmorResistances.keyOf(helmet, chest, null, null));
        assertNotEquals(first, ArmorResistances.keyOf(null, null, null, null));
    }
}
