package org.lytharalab.cabinelevator.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class RoundFloorButton extends Button {
    private boolean current;

    public RoundFloorButton(int x, int y, int diameter, Component message, OnPress onPress,
                             @Nullable Tooltip tooltip) {
        super(x, y, diameter, diameter, message, onPress, DEFAULT_NARRATION);
        setTooltip(tooltip);
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int cx = getX() + getWidth() / 2;
        int cy = getY() + getHeight() / 2;
        int radius = getWidth() / 2;
        int fill;
        int textColor;
        if (!active) {
            fill = GuiUtil.BUTTON_DISABLED;
            textColor = GuiUtil.MUTED;
        } else if (current) {
            fill = isHoveredOrFocused() ? GuiUtil.BUTTON_ACCENT_HOVER : GuiUtil.BUTTON_ACCENT_BG;
            textColor = GuiUtil.BUTTON_ACCENT_TEXT;
        } else {
            fill = isHoveredOrFocused() ? GuiUtil.BUTTON_HOVER : GuiUtil.BUTTON_BG;
            textColor = GuiUtil.TEXT;
        }
        GuiUtil.fillCircle(graphics, cx, cy, radius, current ? GuiUtil.ACCENT : GuiUtil.BORDER);
        if (radius > 0) {
            GuiUtil.fillCircle(graphics, cx, cy, radius - 1, fill);
        }
        Font font = Minecraft.getInstance().font;
        String clipped = font.plainSubstrByWidth(getMessage().getString(), Math.max(0, getWidth() - 4));
        graphics.drawCenteredString(font, clipped, cx, cy - 4, textColor);
    }
}
