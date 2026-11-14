package org.lytharalab.gfbs.doors.client.armor;

import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * Attaches the translucent visor layer to every player renderer.
 *
 * <p>Both built-in skin variants ({@code default} and {@code slim}) get their own renderer, so the
 * layer is added to each one; missing one would mean the visor silently disappears for thin-armed
 * skins. {@code addLayer} appends, which also puts the pane after the armour layer in the render
 * order - the right place for something translucent.</p>
 */
@OnlyIn(Dist.CLIENT)
// EntityRenderersEvent is a MOD bus event: registering this on the default (FORGE) bus means the
// listener is never invoked and the visor layer is never attached.
@EventBusSubscriber(
    modid = "gfbs_misc",
    bus = Bus.MOD,
    value = {Dist.CLIENT}
)
public final class HazmatRenderLayers {
    private HazmatRenderLayers() {
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            PlayerRenderer renderer = event.getPlayerSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new HazmatVisorLayer(renderer));
                renderer.addLayer(new PowerArmorLayer(renderer));
            }
        }
    }
}
