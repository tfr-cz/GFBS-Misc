package org.lytharalab.gfbs.doors.client.armor.compat;

import net.minecraftforge.fml.ModList;

/**
 * Whether Classic Bar is installed, resolved once.
 *
 * <p>Kept free of any Classic Bar import on purpose: the HUD code checks this flag every frame, and touching
 * the adapter class would load its {@code tfar.classicbar} supertypes - which do not exist when the mod is
 * absent.</p>
 */
public final class ClassicBarPresence {
    public static final boolean PRESENT = ModList.get().isLoaded("classicbar");

    private ClassicBarPresence() {
    }
}
