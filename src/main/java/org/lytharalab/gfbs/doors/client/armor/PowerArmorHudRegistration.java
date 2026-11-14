package org.lytharalab.gfbs.doors.client.armor;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Registers the charge rows as the last HUD overlay.
 *
 * <p>{@code RegisterGuiOverlaysEvent} is a mod-bus event, hence the explicit {@code bus}. Registering above the
 * item name puts the rows after every vanilla overlay and after Classic Bar (which registers below that same
 * anchor), which is what keeps other bars from drawing over them.</p>
 */
@EventBusSubscriber(
    modid = "gfbs_misc",
    value = {Dist.CLIENT},
    bus = EventBusSubscriber.Bus.MOD
)
public final class PowerArmorHudRegistration {
    private static final String OVERLAY_ID = "power_armor_charge";

    private PowerArmorHudRegistration() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.ITEM_NAME.id(), OVERLAY_ID, new PowerArmorHud());
    }
}
