package org.lytharalab.cabinelevator.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class WarmEditBox extends EditBox {
    public WarmEditBox(Font font, int x, int y, int width, int height, Component hint) {
        super(font, x, y, width, height, hint);
        setBordered(false);
        setTextColor(GuiUtil.TEXT);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), GuiUtil.FIELD_BG);
        graphics.renderOutline(getX(), getY(), getWidth(), getHeight(),
                isFocused() ? GuiUtil.ACCENT : GuiUtil.FIELD_BORDER);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 5, 0);
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        graphics.pose().popPose();
    }
}
