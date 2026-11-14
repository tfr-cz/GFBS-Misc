package org.lytharalab.gfbs.doors.client.armor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lytharalab.gfbs.doors.content.armor.PowerArmorCharge;

import java.util.List;

/**
 * The green charge bars above the armour row, ported from HBM's
 * {@code ModEventHandlerClient#getBars / onHUDRenderBar}.
 *
 * <p>NTM draws one bar <em>per worn powered piece</em> - not one bar for the set - into the rows right
 * above the armour bar, then bumps {@code GuiIngameForge.left_height} so the vanilla HUD moves up instead of
 * being covered. 1.20.1 has no {@code left_height}, so the bars are drawn from the armour row's own
 * y-coordinate upwards; that keeps them clear of the armour bar without touching vanilla layout.</p>
 *
 * <p>Only the shape is ported: an 81px track at {@code width / 2 - 91}, 3px tall in dark grey, with a 1px
 * bright green fill inset by one pixel. No text, no blinking, no sound - HBM has none of those either.</p>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(
    modid = "gfbs_misc",
    value = {Dist.CLIENT}
)
public final class PowerArmorHud {
    private static final int SEGMENT_WIDTH = 81;
    private static final int INSET = 1;
    private static final int TRACK_HEIGHT = 3;
    private static final int ROW_STEP = 4;
    private static final int TRACK_COLOR = 0xFF404040;
    private static final int FILL_COLOR = 0xFF00FF00;
    /** Vanilla's armour row starts this far above the bottom edge. */
    private static final int ARMOR_ROW_FROM_BOTTOM = 49;
    /** Same left edge the hotbar and the armour row use. */
    private static final int HOTBAR_LEFT_OFFSET = 91;

    private PowerArmorHud() {
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!VanillaGuiOverlay.ARMOR_LEVEL.id().equals(event.getOverlay().id())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        // Inventory#armor is in feet-to-head order, the order the bars stack in.
        List<ItemStack> armor = minecraft.player.getInventory().armor;
        if (!PowerArmorCharge.hasFullSet(armor)) {
            return;
        }

        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        int left = width / 2 - HOTBAR_LEFT_OFFSET;
        int armorRow = height - ARMOR_ROW_FROM_BOTTOM;
        GuiGraphics graphics = event.getGuiGraphics();

        for (int index = 0; index < armor.size(); index++) {
            ItemStack piece = armor.get(index);
            if (!PowerArmorCharge.isPowerArmor(piece)) {
                continue;
            }
            int top = armorRow - ROW_STEP * (index + 1);
            float progress = (float) PowerArmorCharge.get(piece) / (float) PowerArmorCharge.MAX_CHARGE;
            graphics.fill(left, top, left + SEGMENT_WIDTH, top + TRACK_HEIGHT, TRACK_COLOR);
            int fillEnd = fillEnd(left, progress);
            if (fillEnd > left + INSET) {
                graphics.fill(left + INSET, top + INSET, fillEnd, top + INSET + 1, FILL_COLOR);
            }
        }
    }

    /** Where the 1px fill line stops: the track inset by one pixel on both sides, times the charge ratio. */
    static int fillEnd(int left, float progress) {
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        return left + INSET + Math.round((SEGMENT_WIDTH - INSET * 2) * clamped);
    }
}
