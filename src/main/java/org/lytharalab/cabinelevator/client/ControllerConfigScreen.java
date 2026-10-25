package org.lytharalab.cabinelevator.client;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lytharalab.cabinelevator.data.RangeBox;
import org.lytharalab.cabinelevator.network.ControllerSnapshot;
import org.lytharalab.cabinelevator.network.ElevatorNetwork;
import org.lytharalab.cabinelevator.network.SaveControllerPacket;
import org.lytharalab.cabinelevator.util.ElevatorUtil;

import java.util.List;

public class ControllerConfigScreen extends Screen implements StatusReceiver {
    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_HEIGHT = 230;

    private final BlockPos pos;
    private ControllerSnapshot snapshot;
    private String channel = "";
    private String floorName = "";
    private int timeTicks;

    private EditBox floorNameField;
    private EditBox timeField;
    private Button channelButton;
    private Button rangeButton;
    private SimpleList floorList;
    private Component status;
    private long statusUntil;
    private int left;
    private int top;

    public ControllerConfigScreen(ControllerSnapshot snapshot) {
        super(Component.translatable("gui.cabin_elevator.controller.title"));
        this.pos = snapshot.pos;
        applyState(snapshot);
    }

    public boolean matches(BlockPos pos) {
        return this.pos.equals(pos);
    }

    public void applySnapshot(ControllerSnapshot snapshot) {
        applyState(snapshot);
        status = null;
        if (RangeOverlay.isShowing(pos)) {
            RangeOverlay.show(snapshot.range, pos);
        }
        if (minecraft != null) {
            rebuildWidgets();
        }
    }

    private void applyState(ControllerSnapshot snapshot) {
        this.snapshot = snapshot;
        this.channel = snapshot.channel;
        this.floorName = snapshot.floorName;
        this.timeTicks = snapshot.timeTicks;
    }

    @Override
    protected void init() {
        super.init();
        this.left = (width - PANEL_WIDTH) / 2;
        this.top = (height - PANEL_HEIGHT) / 2;

        this.channelButton = new WarmButton(left + 104, top + 24, 140, 20, channelLabel(), button -> openChannelSelect(),
                Tooltip.create(Component.translatable("gui.cabin_elevator.channel.tooltip")));
        addRenderableWidget(channelButton);

        this.floorNameField = new WarmEditBox(font, left + 104, top + 50, 140, 18,
                Component.translatable("gui.cabin_elevator.floor_name"));
        floorNameField.setMaxLength(32);
        floorNameField.setValue(floorName);
        floorNameField.setResponder(value -> floorName = value);
        addRenderableWidget(floorNameField);

        this.rangeButton = new WarmButton(left + 186, top + 74, 64, 18, rangeButtonLabel(), button -> toggleRange(),
                Tooltip.create(Component.translatable("gui.cabin_elevator.range.toggle_tooltip")));
        addRenderableWidget(rangeButton);

        this.timeField = new WarmEditBox(font, left + 104, top + 102, 60, 18,
                Component.translatable("gui.cabin_elevator.time_seconds"));
        timeField.setMaxLength(6);
        timeField.setFilter(value -> value.matches("\\d{0,4}(\\.\\d{0,2})?"));
        timeField.setValue(ElevatorUtil.formatSeconds(timeTicks));
        timeField.setResponder(value -> {
            double seconds = ElevatorUtil.parseSeconds(value);
            if (seconds >= 0.0D) {
                timeTicks = ElevatorUtil.secondsToTicks(seconds);
            }
        });
        addRenderableWidget(timeField);

        WarmButton saveButton = new WarmButton(left + 12, top + 196, 80, 20,
                Component.translatable("gui.cabin_elevator.save"), button -> save(), null);
        saveButton.setAccent(true);
        addRenderableWidget(saveButton);
        addRenderableWidget(new WarmButton(left + 100, top + 196, 60, 20,
                Component.translatable("gui.cabin_elevator.close"), button -> onClose(), null));

        this.floorList = null;
        if (!snapshot.floors.isEmpty()) {
            SimpleList list = new SimpleList(minecraft, 128, 176, top + 40, top + 216, 18);
            list.setLeftPos(left + 260);
            list.setRenderSelection(false);
            for (ControllerSnapshot.FloorInfo floor : snapshot.floors) {
                boolean current = snapshot.currentFloor != null && snapshot.currentFloor.equals(floor.pos);
                boolean here = floor.pos.equals(pos);
                int color = current ? GuiUtil.GOLD : here ? GuiUtil.ACCENT : GuiUtil.TEXT;
                MutableComponent row = Component.literal((floor.index + 1) + ". ")
                        .append(GuiUtil.floorLabel(floor.name, floor.index))
                        .append(Component.literal("  Y=" + floor.pos.getY()));
                if (current) {
                    row = Component.literal("★ ").append(row);
                }
                list.addRow(row, color, floor.pos);
            }
            this.floorList = list;
            addRenderableWidget(list);
        }
    }

    private Component channelLabel() {
        if (channel.isBlank()) {
            return Component.translatable("gui.cabin_elevator.select_channel");
        }
        return Component.literal(font.plainSubstrByWidth(channel, 130));
    }

    private Component rangeButtonLabel() {
        return Component.translatable(RangeOverlay.isShowing(pos)
                ? "gui.cabin_elevator.range.hide" : "gui.cabin_elevator.range.show");
    }

    private void toggleRange() {
        if (RangeOverlay.isShowing(pos)) {
            RangeOverlay.disable();
        } else if (minecraft != null && minecraft.level != null) {
            RangeOverlay.show(minecraft.level.dimension(), snapshot.range, pos);
        }
        if (rangeButton != null) {
            rangeButton.setMessage(rangeButtonLabel());
        }
    }

    private void openChannelSelect() {
        if (minecraft == null) {
            return;
        }
        List<ControllerSnapshot.ChannelInfo> channels = snapshot.channels;
        minecraft.setScreen(new ChannelSelectScreen(this, channels, this::applyChannel, this::applyNewChannel));
    }

    private void applyChannel(ControllerSnapshot.ChannelInfo info) {
        channel = info.name;
        timeTicks = info.timeTicks;
        if (timeField != null) {
            timeField.setValue(ElevatorUtil.formatSeconds(timeTicks));
        }
        showStatus(Component.translatable("gui.cabin_elevator.channel.synced").withStyle(ChatFormatting.GREEN));
    }

    private void applyNewChannel(String name) {
        channel = ElevatorUtil.sanitizeName(name, 32);
        showStatus(Component.translatable("gui.cabin_elevator.channel.new_pending").withStyle(ChatFormatting.GREEN));
    }

    private void save() {
        String newChannel = channel == null ? "" : channel.strip();
        if (newChannel.isEmpty()) {
            localError("message.cabin_elevator.config.channel_required");
            return;
        }
        double seconds = ElevatorUtil.parseSeconds(timeField.getValue());
        if (seconds < 0.0D || seconds > ElevatorUtil.MAX_TIME_TICKS / 20.0D) {
            localError("message.cabin_elevator.config.invalid_time", 0, ElevatorUtil.MAX_TIME_TICKS / 20);
            return;
        }
        int ticks = ElevatorUtil.secondsToTicks(seconds);
        ElevatorNetwork.saveController(new SaveControllerPacket(pos, newChannel, floorNameField.getValue(), ticks));
    }

    private void localError(String key, Object... args) {
        showStatus(Component.translatable(key, args).withStyle(ChatFormatting.RED));
    }

    @Override
    public void showStatus(Component component) {
        this.status = component;
        this.statusUntil = Util.getMillis() + 8000L;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        GuiUtil.panel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.drawCenteredString(font, title, left + PANEL_WIDTH / 2, top + 7, GuiUtil.ACCENT);

        GuiUtil.text(graphics, font, Component.translatable("gui.cabin_elevator.channel"), left + 12, top + 30);
        GuiUtil.text(graphics, font, Component.translatable("gui.cabin_elevator.floor_name"), left + 12, top + 56);
        RangeBox range = snapshot.range;
        GuiUtil.text(graphics, font, Component.translatable("gui.cabin_elevator.range"), left + 12, top + 79);
        graphics.drawString(font, Component.translatable("gui.cabin_elevator.range.value",
                range.width(), range.height(), range.depth()).getString(), left + 104, top + 79, GuiUtil.TEXT, false);
        GuiUtil.muted(graphics, font, Component.translatable("gui.cabin_elevator.range.corners",
                range.minX(), range.maxX() - 1, range.minY(), range.maxY() - 1, range.minZ(), range.maxZ() - 1),
                left + 12, top + 93);
        GuiUtil.text(graphics, font, Component.translatable("gui.cabin_elevator.time_per_floor"), left + 12,
                top + 108);
        GuiUtil.muted(graphics, font, Component.translatable("gui.cabin_elevator.seconds_short"), left + 168,
                top + 108);
        GuiUtil.text(graphics, font, Component.translatable("gui.cabin_elevator.status"), left + 12, top + 150);
        graphics.drawString(font, GuiUtil.stateText(snapshot.idle), left + 52, top + 150, GuiUtil.TEXT, false);
        GuiUtil.muted(graphics, font, Component.translatable("gui.cabin_elevator.config.hint.1"), left + 12,
                top + 166);
        GuiUtil.muted(graphics, font, Component.translatable("gui.cabin_elevator.config.hint.2"), left + 12,
                top + 178);

        if (floorList != null) {
            GuiUtil.subPanel(graphics, left + 254, top + 18, 138, 204);
            GuiUtil.text(graphics, font, Component.translatable("gui.cabin_elevator.floor_list"), left + 260,
                    top + 24);
        } else {
            GuiUtil.subPanel(graphics, left + 254, top + 18, 138, 60);
            GuiUtil.muted(graphics, font, Component.translatable("gui.cabin_elevator.no_floors"), left + 260,
                    top + 24);
        }

        drawStatus(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawStatus(GuiGraphics graphics) {
        if (status != null && Util.getMillis() < statusUntil) {
            graphics.drawString(font, status, left + 12, top + 208, GuiUtil.TEXT, false);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (floorNameField != null) {
            floorNameField.tick();
        }
        if (timeField != null) {
            timeField.tick();
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
