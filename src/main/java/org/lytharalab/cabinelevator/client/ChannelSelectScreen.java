package org.lytharalab.cabinelevator.client;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lytharalab.cabinelevator.network.ControllerSnapshot;
import org.lytharalab.cabinelevator.util.ElevatorUtil;

import java.util.List;
import java.util.function.Consumer;

public class ChannelSelectScreen extends Screen {
    private static final int PANEL_WIDTH = 330;
    private static final int PANEL_HEIGHT = 230;

    private final Screen parent;
    private final List<ControllerSnapshot.ChannelInfo> channels;
    private final Consumer<ControllerSnapshot.ChannelInfo> onSelect;
    private final Consumer<String> onCreate;
    private SimpleList list;
    private EditBox nameField;
    private Component status;
    private long statusUntil;
    private int left;
    private int top;

    public ChannelSelectScreen(Screen parent, List<ControllerSnapshot.ChannelInfo> channels,
                               Consumer<ControllerSnapshot.ChannelInfo> onSelect, Consumer<String> onCreate) {
        super(Component.translatable("gui.cabin_elevator.channel_select.title"));
        this.parent = parent;
        this.channels = channels;
        this.onSelect = onSelect;
        this.onCreate = onCreate;
    }

    @Override
    protected void init() {
        super.init();
        this.left = (width - PANEL_WIDTH) / 2;
        this.top = (height - PANEL_HEIGHT) / 2;

        this.list = new SimpleList(minecraft, 306, 110, top + 40, top + 150, 20);
        this.list.setLeftPos(left + 12);
        for (ControllerSnapshot.ChannelInfo info : channels) {
            String floorText = info.floorsHere == info.floorsTotal
                    ? Component.translatable("gui.cabin_elevator.channel.floors_here", info.floorsHere).getString()
                    : Component.translatable("gui.cabin_elevator.channel.floors_split", info.floorsHere,
                    info.floorsTotal).getString();
            Component row = Component.literal(info.name + "  ·  " + info.range.width() + "×" + info.range.height()
                    + "×" + info.range.depth() + "  ·  " + ElevatorUtil.formatSeconds(info.timeTicks) + "s  ·  "
                    + floorText);
            list.addRow(row, GuiUtil.TEXT, null);
        }
        list.setRowClick(row -> useSelected());
        addRenderableWidget(list);

        this.nameField = new WarmEditBox(font, left + 12, top + 158, 210, 18,
                Component.translatable("gui.cabin_elevator.channel.new"));
        nameField.setMaxLength(32);
        nameField.setHint(Component.translatable("gui.cabin_elevator.channel.new_hint"));
        addRenderableWidget(nameField);

        WarmButton createButton = new WarmButton(left + 232, top + 157, 86, 20,
                Component.translatable("gui.cabin_elevator.channel.create"), button -> createChannel(), null);
        createButton.setAccent(true);
        addRenderableWidget(createButton);
        addRenderableWidget(new WarmButton(left + 12, top + 184, 110, 20,
                Component.translatable("gui.cabin_elevator.channel.use_selected"), button -> useSelected(), null));
        addRenderableWidget(new WarmButton(left + 130, top + 184, 70, 20,
                Component.translatable("gui.cabin_elevator.back"), button -> back(), null));
    }

    private void useSelected() {
        SimpleList.Row selected = list == null ? null : list.getSelected();
        int index = selected == null || list == null ? -1 : list.children().indexOf(selected);
        if (index < 0 || index >= channels.size()) {
            showStatus(Component.translatable("gui.cabin_elevator.channel.select_first")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        ControllerSnapshot.ChannelInfo info = channels.get(index);
        onSelect.accept(info);
        back();
    }

    private void createChannel() {
        String name = ElevatorUtil.sanitizeName(nameField.getValue(), 32);
        if (name.isBlank()) {
            showStatus(Component.translatable("message.cabin_elevator.config.channel_required")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        onCreate.accept(name);
        back();
    }

    private void back() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void showStatus(Component component) {
        status = component;
        statusUntil = Util.getMillis() + 6000L;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        GuiUtil.panel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.drawCenteredString(font, title, left + PANEL_WIDTH / 2, top + 7, GuiUtil.ACCENT);
        GuiUtil.muted(graphics, font, Component.translatable("gui.cabin_elevator.channel_select.hint"),
                left + 12, top + 24);
        GuiUtil.subPanel(graphics, left + 8, top + 36, 314, 118);
        if (channels.isEmpty()) {
            GuiUtil.muted(graphics, font, Component.translatable("gui.cabin_elevator.channel.none"),
                    left + 16, top + 48);
        }
        if (status != null && Util.getMillis() < statusUntil) {
            graphics.drawString(font, status, left + 12, top + 210, GuiUtil.TEXT, false);
        } else {
            GuiUtil.muted(graphics, font, Component.translatable("gui.cabin_elevator.channel_select.footer"),
                    left + 12, top + 210);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        super.tick();
        if (nameField != null) {
            nameField.tick();
        }
    }

    @Override
    public void onClose() {
        back();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
