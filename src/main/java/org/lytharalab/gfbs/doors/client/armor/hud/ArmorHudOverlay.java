package org.lytharalab.gfbs.doors.client.armor.hud;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * A screen overlay an armour piece can contribute while it is worn - the port of HBM's
 * {@code ArmorFSB#handleOverlay} / {@code setOverlay(texture)} pair.
 *
 * <p>HBM let an armour set draw a full-screen helmet overlay (visor, HUD tick marks, gauges) by handing
 * the overlay a texture and letting it paint over the game view. This is the same idea with the drawing
 * left to the implementation, so a set can render whatever it needs instead of being limited to a
 * stretched texture.</p>
 *
 * <p>Register one per item with {@link ArmorHudOverlays#register}; the dispatcher only looks at the
 * helmet slot, matching HBM, where the overlay came from the helmet piece.</p>
 */
@OnlyIn(Dist.CLIENT)
@FunctionalInterface
public interface ArmorHudOverlay {
    /**
     * @param context everything an overlay needs: the draw context, the player, the screen size and the
     *                frame's partial tick
     */
    void render(ArmorHudContext context);
}
