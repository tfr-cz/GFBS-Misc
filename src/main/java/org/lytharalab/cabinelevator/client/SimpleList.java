package org.lytharalab.cabinelevator.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class SimpleList extends ObjectSelectionList<SimpleList.Row> {
    private static final int ROW_BG = 0x28C9A66A;
    private static final int ROW_BG_HOVER = 0x50E3C58C;
    private static final int ROW_BG_SELECTED = 0x78E3C58C;

    @Nullable
    private Consumer<Row> rowClick;

    public SimpleList(Minecraft minecraft, int width, int height, int top, int bottom, int itemHeight) {
        super(minecraft, width, height, top, bottom, itemHeight);
        setRenderBackground(false);
        setRenderTopAndBottom(false);
    }

    public void setRowClick(@Nullable Consumer<Row> rowClick) {
        this.rowClick = rowClick;
    }

    @Override
    public int getRowWidth() {
        return Math.max(40, getWidth() - 12);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (button == 0 && rowClick != null && mouseX >= getLeft() && mouseX < getLeft() + getWidth()
                && mouseY >= getTop() && mouseY < getTop() + getHeight()) {
            Row clicked = getEntryAtPosition(mouseX, mouseY);
            if (clicked != null) {
                setSelected(clicked);
                rowClick.accept(clicked);
                return true;
            }
        }
        return handled;
    }

    public Row addRow(Component text, int color, @Nullable BlockPos pos) {
        Row row = new Row(text, color, pos);
        addEntry(row);
        return row;
    }

    public void clearRows() {
        clearEntries();
    }

    public class Row extends ObjectSelectionList.Entry<Row> {
        private final Component text;
        private final int color;
        @Nullable
        private final BlockPos pos;

        public Row(Component text, int color, @Nullable BlockPos pos) {
            this.text = text;
            this.color = color;
            this.pos = pos;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX,
                           int mouseY, boolean hovering, float partialTick) {
            boolean selected = SimpleList.this.getSelected() == this;
            int background = selected ? ROW_BG_SELECTED : hovering ? ROW_BG_HOVER : ROW_BG;
            graphics.fill(left, top, left + width, top + height, background);
            if (selected) {
                graphics.renderOutline(left, top, width, height, GuiUtil.ACCENT);
            }
            String clipped = Minecraft.getInstance().font.plainSubstrByWidth(text.getString(),
                    Math.max(10, width - 8));
            graphics.drawString(Minecraft.getInstance().font, clipped, left + 4, top + (height - 8) / 2,
                    color, false);
        }

        @Override
        public Component getNarration() {
            return text;
        }

        @Nullable
        public BlockPos pos() {
            return pos;
        }
    }
}
