package org.lytharalab.cabinelevator.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class WarmButton extends Button {
    private boolean accent;

    public WarmButton(int x, int y, int width, int height, Component message, OnPress onPress,
                      @Nullable Tooltip tooltip) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        setTooltip(tooltip);
    }

    public void setAccent(boolean accent) {
        this.accent = accent;
    }

    public boolean isAccent() {
        return accent;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int background;
        int textColor;
        if (!active) {
            background = GuiUtil.BUTTON_DISABLED;
            textColor = GuiUtil.MUTED;
        } else if (accent) {
            background = isHoveredOrFocused() ? GuiUtil.BUTTON_ACCENT_HOVER : GuiUtil.BUTTON_ACCENT_BG;
            textColor = GuiUtil.BUTTON_ACCENT_TEXT;
        } else {
            background = isHoveredOrFocused() ? GuiUtil.BUTTON_HOVER : GuiUtil.BUTTON_BG;
            textColor = GuiUtil.TEXT;
        }
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), background);
        graphics.renderOutline(getX(), getY(), getWidth(), getHeight(),
                accent ? GuiUtil.ACCENT : GuiUtil.BORDER);
        Font font = Minecraft.getInstance().font;
        int textX = getX() + Math.max(0, (getWidth() - font.width(getMessage())) / 2);
        int textY = getY() + (getHeight() - 8) / 2;
        String clipped = font.plainSubstrByWidth(getMessage().getString(), Math.max(0, getWidth() - 4));
        graphics.drawString(font, clipped, textX, textY, textColor, false);
    }
}
