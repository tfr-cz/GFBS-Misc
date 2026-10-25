package org.lytharalab.cabinelevator.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import org.lytharalab.cabinelevator.util.ElevatorUtil;

public final class GuiUtil {
    public static final int PANEL_BG = 0xFFF4EDDC;
    public static final int SUB_PANEL_BG = 0xFFE7DCC2;
    public static final int BORDER = 0xFFB08A54;
    public static final int ACCENT = 0xFFA96F32;
    public static final int TEXT = 0xFF4C3B28;
    public static final int MUTED = 0xFF8C7A62;
    public static final int ERROR = 0xFFB0392E;
    public static final int SUCCESS = 0xFF4E7A38;
    public static final int GOLD = 0xFFB8862B;
    public static final int FIELD_BG = 0xFFFDF6E7;
    public static final int FIELD_BORDER = 0xFFC9A66B;
    public static final int BUTTON_BG = 0xFFEFE1C8;
    public static final int BUTTON_HOVER = 0xFFF6EBD8;
    public static final int BUTTON_DISABLED = 0xFFDCD2BD;
    public static final int BUTTON_ACCENT_BG = 0xFFC97F4A;
    public static final int BUTTON_ACCENT_HOVER = 0xFFD89058;
    public static final int BUTTON_ACCENT_TEXT = 0xFFFCF4E4;
    public static final int LED_BG = 0xFF33291A;
    public static final int LED_TEXT = 0xFFE8B04B;

    private GuiUtil() {
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL_BG);
        graphics.renderOutline(x, y, width, height, BORDER);
    }

    public static void subPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, SUB_PANEL_BG);
        graphics.renderOutline(x, y, width, height, 0xFFC8AF83);
    }

    public static void ledPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, LED_BG);
        graphics.renderOutline(x, y, width, height, 0xFF6B5433);
    }

    public static void fillCircle(GuiGraphics graphics, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int half = (int) Math.round(Math.sqrt((double) radius * radius - (double) dy * dy));
            graphics.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    public static void bigCenteredNumber(GuiGraphics graphics, Font font, Component value, int centerX, int y,
                                         float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawCenteredString(font, value, 0, 0, color);
        graphics.pose().popPose();
    }

    public static void text(GuiGraphics graphics, Font font, Component component, int x, int y) {
        graphics.drawString(font, component, x, y, TEXT, false);
    }

    public static void muted(GuiGraphics graphics, Font font, Component component, int x, int y) {
        graphics.drawString(font, component, x, y, MUTED, false);
    }

    public static Component seconds(int ticks) {
        return Component.literal(ElevatorUtil.formatSeconds(ticks) + "s");
    }

    public static Component direction(Direction direction) {
        return ElevatorUtil.directionName(direction);
    }

    public static Component stateText(boolean idle) {
        return Component.translatable(idle ? "gui.cabin_elevator.state.idle" : "gui.cabin_elevator.state.busy")
                .withStyle(idle ? ChatFormatting.GREEN : ChatFormatting.GOLD);
    }

    public static Component floorLabel(String name, int index) {
        if (name != null && !name.isBlank()) {
            return Component.literal(name);
        }
        return Component.translatable("gui.cabin_elevator.floor_fallback", index + 1);
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
