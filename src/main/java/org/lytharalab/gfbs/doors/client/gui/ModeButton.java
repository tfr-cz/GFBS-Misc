package org.lytharalab.gfbs.doors.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class ModeButton extends AbstractWidget {
    private final Runnable onPress;
    private boolean selected;

    public ModeButton(int x, int y, int width, int height, Component message, boolean selected, Runnable onPress, @Nullable Tooltip tooltip) {
        super(x, y, width, height, message);
        this.selected = selected;
        this.onPress = onPress;
        this.setTooltip(tooltip);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return this.selected;
    }

    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean hovered = this.isHoveredOrFocused() && this.active;
        int background;
        int border;
        int textColor;
        if (!this.active) {
            background = -14473687;
            border = -12696755;
            textColor = -10722967;
        } else if (this.selected) {
            background = -12301739;
            border = -2513855;
            textColor = -1512723;
        } else if (hovered) {
            background = -12959927;
            border = -12696755;
            textColor = -1512723;
        } else {
            background = -13749702;
            border = -12696755;
            textColor = -1512723;
        }

        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();
        g.fill(x, y, x + w, y + h, background);
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, border);
        Font font = Minecraft.getInstance().font;
        String label = this.getMessage().getString();
        String clipped = font.plainSubstrByWidth(label, Math.max(0, w - 8));
        int textX = x + Math.max(0, (w - font.width(clipped)) / 2);
        int textY = y + (h - 8) / 2;
        g.drawString(font, clipped, textX, textY, textColor, false);
    }

    public void onClick(double mouseX, double mouseY) {
        if (this.active) {
            this.onPress.run();
        }
    }

    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
