package org.lytharalab.gfbs.doors.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class Draw {
    private Draw() {
    }

    public static void box(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
    }

    public static void frame(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static void panel(GuiGraphics g, int x, int y, int w, int h, int bg, int border) {
        box(g, x, y, w, h, bg);
        frame(g, x, y, w, h, border);
    }

    public static void rule(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, -12696755);
    }

    public static void sectionTitle(GuiGraphics g, Font font, Component text, int x, int y) {
        g.drawString(font, text, x, y, -6577748, false);
    }

    public static void text(GuiGraphics g, Font font, Component text, int x, int y) {
        g.drawString(font, text, x, y, -1512723, false);
    }

    public static void dim(GuiGraphics g, Font font, Component text, int x, int y) {
        g.drawString(font, text, x, y, -6577748, false);
    }

    public static void colored(GuiGraphics g, Font font, Component text, int x, int y, int color) {
        g.drawString(font, text, x, y, color, false);
    }

    public static void status(GuiGraphics g, Font font, Component text, int x, int y, int color) {
        g.fill(x, y + 3, x + 3, y + 6, color);
        g.drawString(font, text, x + 7, y, color, false);
    }

    public static void keyValue(GuiGraphics g, Font font, Component key, Component value, int x, int y, int w, int valueColor) {
        g.drawString(font, key, x, y, -6577748, false);
        g.drawString(font, value, x + w - font.width(value), y, valueColor, false);
    }
}
