package org.lytharalab.gfbs.doors.client.armor.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** What an {@link ArmorHudOverlay} gets to draw with. */
@OnlyIn(Dist.CLIENT)
public record ArmorHudContext(GuiGraphics graphics, Player player, float partialTick, int width, int height) {
}
