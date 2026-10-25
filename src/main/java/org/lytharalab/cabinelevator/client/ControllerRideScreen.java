package org.lytharalab.cabinelevator.client;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lytharalab.cabinelevator.network.ControllerSnapshot;
import org.lytharalab.cabinelevator.network.ElevatorNetwork;
import org.lytharalab.cabinelevator.util.ElevatorUtil;

import java.util.ArrayList;
import java.util.List;

public class ControllerRideScreen extends Screen implements StatusReceiver {
    private static final int PANEL_WIDTH = 230;
    private static final int LED_WIDTH = 72;
    private static final int LED_HEIGHT = 28;
    private static final int HEADER_HEIGHT = 52;
    private static final int DOOR_DIAMETER = 30;

    private final BlockPos pos;
    private ControllerSnapshot snapshot;
    private String channel = "";
    private int timeTicks;
    private int sourceIndex = -1;
    private int currentFloorIndex = -1;
    private boolean idle = true;
    private boolean inRange;
    private int buttonSize;
    private int buttonGap;
    private final List<RoundFloorButton> floorButtons = new ArrayList<>();
    private RoundFloorButton doorButton;
    private Component status;
    private long statusUntil;
    private int left;
    private int top;
    private int gridX;
    private int gridY;
    private int contentHeight;
    private int nameX;
    private int nameWidth;
    private int statusY;
    private int buttonsY;
    private int panelHeight;

    public ControllerRideScreen(ControllerSnapshot snapshot) {
        super(Component.translatable("gui.cabin_elevator.ride.title"));
        this.pos = snapshot.pos;
        applyState(snapshot);
    }

    public boolean matches(BlockPos pos) {
        return this.pos.equals(pos);
    }

    public void applySnapshot(ControllerSnapshot snapshot) {
        applyState(snapshot);
        status = null;
        if (minecraft != null) {
            rebuildWidgets();
        }
    }

    private void applyState(ControllerSnapshot snapshot) {
        this.snapshot = snapshot;
        this.channel = snapshot.channel;
        this.timeTicks = snapshot.timeTicks;
        this.idle = snapshot.idle;
        this.sourceIndex = -1;
        this.currentFloorIndex = -1;
        for (ControllerSnapshot.FloorInfo floor : snapshot.floors) {
            if (floor.pos.equals(pos)) {
                this.sourceIndex = floor.index;
            }
            if (snapshot.currentFloor != null && floor.pos.equals(snapshot.currentFloor)) {
                this.currentFloorIndex = floor.index;
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        this.left = (width - PANEL_WIDTH) / 2;
        int floorCount = snapshot.floors.size();
        this.buttonSize = 20;
        this.buttonGap = 8;
        if (floorCount > 0 && HEADER_HEIGHT + (floorCount * 28 - 8) + 57 > height - 6) {
            this.buttonSize = 16;
            this.buttonGap = 4;
        }
        this.contentHeight = floorCount == 0 ? 40 : floorCount * (buttonSize + buttonGap) - buttonGap;
        this.panelHeight = HEADER_HEIGHT + contentHeight + 19 + DOOR_DIAMETER + 8;
        this.top = Math.max(2, (height - panelHeight) / 2);
        this.gridX = left + 12;
        this.gridY = top + HEADER_HEIGHT;
        this.nameX = gridX + buttonSize + 10;
        this.nameWidth = Math.max(40, left + PANEL_WIDTH - 12 - nameX);
        this.statusY = gridY + contentHeight + 6;
        this.buttonsY = statusY + 13;
        this.inRange = computeInRange();

        floorButtons.clear();
        boolean enabled = idle && inRange && sourceIndex >= 0 && floorCount > 1;
        for (ControllerSnapshot.FloorInfo floor : snapshot.floors) {
            int rowFromTop = floorCount - 1 - floor.index;
            int bx = gridX;
            int by = gridY + rowFromTop * (buttonSize + buttonGap);
            MutableComponent tooltip = GuiUtil.floorLabel(floor.name, floor.index).copy();
            if (sourceIndex >= 0 && floor.index != sourceIndex) {
                tooltip = tooltip.copy().append(Component.literal(
                        " · " + ElevatorUtil.formatSeconds(Math.abs(floor.index - sourceIndex) * timeTicks) + "s"));
            }
            RoundFloorButton button = new RoundFloorButton(bx, by, buttonSize,
                    Component.literal(String.valueOf(floor.index + 1)),
                    pressed -> pressFloor(floor), Tooltip.create(tooltip));
            button.setCurrent(floor.index == currentFloorIndex);
            button.active = enabled;
            floorButtons.add(button);
            addRenderableWidget(button);
        }

        this.doorButton = new RoundFloorButton(left + 12, buttonsY, DOOR_DIAMETER,
                Component.translatable("gui.cabin_elevator.ride.open_door"),
                pressed -> openDoor(),
                Tooltip.create(Component.translatable("gui.cabin_elevator.ride.door_tooltip")));
        doorButton.setCurrent(true);
        doorButton.active = idle && !channel.isBlank();
        addRenderableWidget(doorButton);
        addRenderableWidget(new WarmButton(left + PANEL_WIDTH - 12 - 44, buttonsY + (DOOR_DIAMETER - 16) / 2,
                44, 16, Component.translatable("gui.cabin_elevator.close"), button -> onClose(), null));
    }

    private boolean computeInRange() {
        if (minecraft == null || minecraft.player == null) {
            return false;
        }
        return minecraft.player.getBoundingBox().intersects(ElevatorUtil.rangeAabb(pos, snapshot.range));
    }

    private void pressFloor(ControllerSnapshot.FloorInfo floor) {
        if (!idle) {
            showStatus(Component.translatable("message.cabin_elevator.busy").withStyle(ChatFormatting.GOLD));
            return;
        }
        if (!inRange) {
            showStatus(Component.translatable("message.cabin_elevator.out_of_range").withStyle(ChatFormatting.RED));
            return;
        }
        if (sourceIndex < 0) {
            showStatus(Component.translatable("message.cabin_elevator.not_configured")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        if (floor.pos.equals(pos)) {
            showStatus(Component.translatable("message.cabin_elevator.same_floor").withStyle(ChatFormatting.RED));
            return;
        }
        showStatus(Component.translatable("gui.cabin_elevator.ride.requested",
                GuiUtil.floorLabel(floor.name, floor.index)).withStyle(ChatFormatting.GREEN));
        ElevatorNetwork.requestTravel(pos, floor.pos);
    }

    private void openDoor() {
        if (!idle) {
            showStatus(Component.translatable("message.cabin_elevator.busy").withStyle(ChatFormatting.GOLD));
            return;
        }
        if (channel.isBlank()) {
            showStatus(Component.translatable("message.cabin_elevator.not_configured")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        ElevatorNetwork.requestDoorOpen(pos);
    }

    @Override
    public void showStatus(Component component) {
        this.status = component;
        this.statusUntil = Util.getMillis() + 8000L;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        GuiUtil.panel(graphics, left, top, PANEL_WIDTH, panelHeight);
        inRange = computeInRange();
        boolean floorsEnabled = idle && inRange && sourceIndex >= 0 && snapshot.floors.size() > 1;
        for (RoundFloorButton button : floorButtons) {
            button.active = floorsEnabled;
        }
        if (doorButton != null) {
            doorButton.active = idle && !channel.isBlank();
        }

        Component channelText = Component.literal(clip(channel.isBlank()
                ? Component.translatable("gui.cabin_elevator.not_set").getString() : channel, 120));
        GuiUtil.muted(graphics, font, Component.translatable("gui.cabin_elevator.ride.subtitle", channelText,
                GuiUtil.stateText(idle)), left + 12, top + 16);

        int ledX = left + PANEL_WIDTH - 12 - LED_WIDTH;
        int ledY = top + 8;
        GuiUtil.ledPanel(graphics, ledX, ledY, LED_WIDTH, LED_HEIGHT);
        Component currentNumber = currentFloorIndex < 0
                ? Component.literal("—") : Component.literal(String.valueOf(currentFloorIndex + 1));
        GuiUtil.bigCenteredNumber(graphics, font, currentNumber, ledX + LED_WIDTH / 2, ledY + 6,
                2.0F, GuiUtil.LED_TEXT);
        graphics.drawCenteredString(font, Component.translatable("gui.cabin_elevator.ride.current_floor"),
                ledX + LED_WIDTH / 2, ledY + LED_HEIGHT + 3, GuiUtil.MUTED);

        if (snapshot.floors.isEmpty()) {
            GuiUtil.muted(graphics, font, Component.translatable("gui.cabin_elevator.ride.no_floors"),
                    left + 12, gridY + 4);
        } else {
            for (ControllerSnapshot.FloorInfo floor : snapshot.floors) {
                int rowFromTop = snapshot.floors.size() - 1 - floor.index;
                int textY = gridY + rowFromTop * (buttonSize + buttonGap) + (buttonSize - 8) / 2;
                int color = floor.index == currentFloorIndex ? GuiUtil.GOLD : GuiUtil.TEXT;
                graphics.drawString(font, clip(GuiUtil.floorLabel(floor.name, floor.index).getString(),
                        nameWidth), nameX, textY, color, false);
            }
        }

        if (status != null && Util.getMillis() < statusUntil) {
            graphics.drawString(font, clip(status.getString(), PANEL_WIDTH - 24), left + 12, statusY,
                    GuiUtil.TEXT, false);
        } else if (!inRange) {
            GuiUtil.muted(graphics, font, Component.translatable("message.cabin_elevator.out_of_range"),
                    left + 12, statusY);
        } else if (snapshot.floors.size() <= 1) {
            GuiUtil.muted(graphics, font, Component.translatable("gui.cabin_elevator.ride.one_floor"),
                    left + 12, statusY);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private String clip(String text, int pixelWidth) {
        return text == null ? "" : font.plainSubstrByWidth(text, pixelWidth);
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
