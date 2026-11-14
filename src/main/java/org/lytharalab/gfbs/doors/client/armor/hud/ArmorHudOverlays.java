package org.lytharalab.gfbs.doors.client.armor.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dispatch point for helmet overlays: register an {@link ArmorHudOverlay} per item, it is drawn while
 * that item is in the player's helmet slot.
 *
 * <p>HBM kept one overlay per armour piece and rendered it from a {@code RenderGameOverlayEvent.Pre}
 * handler; this is the same shape, hung off Forge's {@code RenderGuiOverlayEvent.Post} so overlays paint
 * on top of the finished HUD rather than in the middle of it. Registration is per item on purpose: when
 * more armour sets are ported, each one brings its own overlay instead of this class growing a switch.</p>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(
    modid = "gfbs_misc",
    value = {Dist.CLIENT}
)
public final class ArmorHudOverlays {
    private static final Map<Item, ArmorHudOverlay> OVERLAYS = new ConcurrentHashMap<>();

    private ArmorHudOverlays() {
    }

    /** Registers (or replaces) the overlay for an item. Safe to call from client setup. */
    public static void register(Item item, ArmorHudOverlay overlay) {
        OVERLAYS.put(item, overlay);
    }

    public static void unregister(Item item) {
        OVERLAYS.remove(item);
    }

    /** The overlay for a worn stack, or {@code null}. */
    @Nullable
    public static ArmorHudOverlay overlayFor(ItemStack stack) {
        return stack.isEmpty() ? null : OVERLAYS.get(stack.getItem());
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        ArmorHudOverlay overlay = overlayFor(minecraft.player.getItemBySlot(EquipmentSlot.HEAD));
        if (overlay == null) {
            return;
        }
        overlay.render(new ArmorHudContext(event.getGuiGraphics(), minecraft.player, event.getPartialTick(),
            event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight()));
    }
}
