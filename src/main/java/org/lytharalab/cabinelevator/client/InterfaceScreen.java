package org.lytharalab.cabinelevator.client;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lytharalab.cabinelevator.content.block.RedstoneInterfaceBlock;
import org.lytharalab.cabinelevator.network.ElevatorNetwork;
import org.lytharalab.cabinelevator.network.InterfaceSnapshot;
import org.lytharalab.cabinelevator.network.SaveInterfacePacket;
import org.lytharalab.cabinelevator.util.ElevatorUtil;

import java.util.ArrayList;
import java.util.List;

public class InterfaceScreen extends Screen implements StatusReceiver {
    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_HEIGHT = 252;
    private static final Direction[] FACES = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH,
            Direction.WEST, Direction.UP, Direction.DOWN
    };

    private final BlockPos pos;
    private InterfaceSnapshot snapshot;
    private int priority;
    private int facesMask;
    private int strength;
    private int durationTicks;
    private int nextDelayTicks;
    private int arrivalDelayTicks;

    private EditBox priorityField;
    private EditBox strengthField;
    private EditBox durationField;
    private EditBox nextDelayField;
    private EditBox arrivalDelayField;
    private final List<WarmButton> faceButtons = new ArrayList<>();
    private SimpleList interfaceList;
    private Component status;
    private long statusUntil;
    private int left;
    private int top;

    public InterfaceScreen(InterfaceSnapshot snapshot) {
        super(Component.translatable("gui.cabin_elevator.interface.title"));
        this.pos = snapshot.pos;
        applyState(snapshot);
    }

    public boolean matches(BlockPos pos) {
        return this.pos.equals(pos);
    }

    public void applySnapshot(InterfaceSnapshot snapshot) {
        applyState(snapshot);
        status = null;
        if (minecraft != null) {
            rebuildWidgets();
        }
    }

    private void applyState(InterfaceSnapshot snapshot) {
        this.snapshot = snapshot;
        this.priority = snapshot.priority;
        this.facesMask = snapshot.facesMask;
        this.strength = snapshot.strength;
        this.durationTicks = snapshot.durationTicks;
        this.nextDelayTicks = snapshot.nextDelayTicks;
        this.arrivalDelayTicks = snapshot.arrivalDelayTicks;
    }

    @Override
    protected void init() {
        super.init();
        this.left = (width - PANEL_WIDTH) / 2;
        this.top = (height - PANEL_HEIGHT) / 2;
        faceButtons.clear();

        addRenderableWidget(new WarmButton(left + 70, top + 48, 18, 18,
                Component.literal("-"), button -> setPriority(priority - 1), null));
        this.priorityField = numericField(left + 90, top + 48, 38, 3, String.valueOf(priority));
        priorityField.setResponder(value -> {
            int parsed = GuiUtil.parseInt(value, -1);
            if (parsed >= ElevatorUtil.MIN_PRIORITY && parsed <= ElevatorUtil.MAX_PRIORITY) {
                priority = parsed;
                updateArrivalVisibility();
            }
        });
        addRenderableWidget(priorityField);
        addRenderableWidget(new WarmButton(left + 130, top + 48, 18, 18,
                Component.literal("+"), button -> setPriority(priority + 1), null));

        for (int i = 0; i < FACES.length; i++) {
            Direction face = FACES[i];
            int column = i % 3;
            int row = i / 3;
            WarmButton button = new WarmButton(left + 78 + column * 54, top + 72 + row * 22, 50, 18,
                    faceLabel(face), b -> toggleFace(face),
                    Tooltip.create(Component.translatable("gui.cabin_elevator.interface.face_tooltip")));
            button.setAccent(RedstoneInterfaceBlock.hasFace(facesMask, face));
            faceButtons.add(button);
            addRenderableWidget(button);
        }

        addRenderableWidget(new WarmButton(left + 70, top + 118, 18, 18,
                Component.literal("-"), button -> setStrength(strength - 1), null));
        this.strengthField = numericField(left + 90, top + 118, 38, 2, String.valueOf(strength));
        strengthField.setResponder(value -> {
            int parsed = GuiUtil.parseInt(value, -1);
            if (parsed >= 0 && parsed <= 15) {
                strength = parsed;
            }
        });
        addRenderableWidget(strengthField);
        addRenderableWidget(new WarmButton(left + 130, top + 118, 18, 18,
                Component.literal("+"), button -> setStrength(strength + 1), null));

        this.durationField = secondsField(left + 104, top + 142, durationTicks);
        durationField.setResponder(value -> {
            double seconds = ElevatorUtil.parseSeconds(value);
            if (seconds >= 0.0D) {
                durationTicks = ElevatorUtil.secondsToTicks(seconds);
            }
        });
        addRenderableWidget(durationField);
        this.nextDelayField = secondsField(left + 104, top + 166, nextDelayTicks);
        nextDelayField.setResponder(value -> {
            double seconds = ElevatorUtil.parseSeconds(value);
            if (seconds >= 0.0D) {
                nextDelayTicks = ElevatorUtil.secondsToTicks(seconds);
            }
        });
        addRenderableWidget(nextDelayField);
        this.arrivalDelayField = secondsField(left + 104, top + 190, arrivalDelayTicks);
        arrivalDelayField.setResponder(value -> {
            double seconds = ElevatorUtil.parseSeconds(value);
            if (seconds >= 0.0D) {
                arrivalDelayTicks = ElevatorUtil.secondsToTicks(seconds);
            }
        });
        addRenderableWidget(arrivalDelayField);
        updateArrivalVisibility();

        WarmButton saveButton = new WarmButton(left + 12, top + 216, 80, 20,
                Component.translatable("gui.cabin_elevator.save"), button -> save(), null);
        saveButton.setAccent(true);
        addRenderableWidget(saveButton);
        addRenderableWidget(new WarmButton(left + 100, top + 216, 60, 20,
                Component.translatable("gui.cabin_elevator.close"), button -> onClose(), null));

        this.interfaceList = null;
        if (!snapshot.interfaces.isEmpty()) {
            SimpleList list = new SimpleList(minecraft, 132, 148, top + 58, top + 206, 13);
            list.setLeftPos(left + 256);
            list.setRenderSelection(false);
            for (InterfaceSnapshot.BoundInterface ref : snapshot.interfaces) {
                boolean current = ref.pos.equals(pos);
                int color = !ref.valid ? GuiUtil.ERROR : current ? GuiUtil.ACCENT : GuiUtil.TEXT;
                MutableComponent row = Component.literal((current ? "* " : "  ") + "P" + ref.priority + "  ("
                        + ref.pos.getX() + "," + ref.pos.getY() + "," + ref.pos.getZ() + ")");
                list.addRow(row, color, ref.pos);
            }
            this.interfaceList = list;
            addRenderableWidget(list);
        }
    }

    private EditBox numericField(int x, int y, int width, int maxLength, String value) {
        EditBox box = new WarmEditBox(font, x, y, width, 18, Component.empty());
        box.setMaxLength(maxLength);
        box.setFilter(text -> text.matches("\\d*"));
        box.setValue(value);
        return box;
    }

    private EditBox secondsField(int x, int y, int ticks) {
        EditBox box = new WarmEditBox(font, x, y, 48, 18, Component.empty());
        box.setMaxLength(6);
        box.setFilter(text -> text.matches("\\d{0,4}(\\.\\d{0,2})?"));
        box.setValue(ElevatorUtil.formatSeconds(ticks));
        return box;
    }

    private void updateArrivalVisibility() {
        if (arrivalDelayField == null) {
            return;
        }
        boolean visible = priority == 1;
        arrivalDelayField.setVisible(visible);
        arrivalDelayField.active = visible;
    }

    private void setPriority(int value) {
        priority = GuiUtil.clamp(value, ElevatorUtil.MIN_PRIORITY, ElevatorUtil.MAX_PRIORITY);
        if (priorityField != null) {
            priorityField.setValue(String.valueOf(priority));
        }
        updateArrivalVisibility();
    }

    private void setStrength(int value) {
        strength = GuiUtil.clamp(value, 0, 15);
        if (strengthField != null) {
            strengthField.setValue(String.valueOf(strength));
        }
    }

    private void toggleFace(Direction face) {
        facesMask ^= 1 << face.get3DDataValue();
        if (facesMask == 0) {
            facesMask = 1 << face.get3DDataValue();
        }
        refreshFaceButtons();
    }

    private void refreshFaceButtons() {
        for (int i = 0; i < FACES.length && i < faceButtons.size(); i++) {
            Direction face = FACES[i];
            faceButtons.get(i).setMessage(faceLabel(face));
            faceButtons.get(i).setAccent(RedstoneInterfaceBlock.hasFace(facesMask, face));
        }
    }

    private Component faceLabel(Direction face) {
        boolean on = (facesMask & 1 << face.get3DDataValue()) != 0;
        return Component.translatable(on ? "gui.cabin_elevator.interface.face_on"
                        : "gui.cabin_elevator.interface.face_off",
                GuiUtil.direction(face));
    }

    private void save() {
        int newPriority = GuiUtil.parseInt(priorityField.getValue(), -1);
        if (newPriority < ElevatorUtil.MIN_PRIORITY || newPriority > ElevatorUtil.MAX_PRIORITY) {
            localError("message.cabin_elevator.interface.invalid_priority");
            return;
        }
        if (facesMask == 0) {
            localError("message.cabin_elevator.interface.no_faces");
            return;
        }
        int newStrength = GuiUtil.parseInt(strengthField.getValue(), -1);
        if (newStrength < 0 || newStrength > 15) {
            localError("message.cabin_elevator.interface.invalid_strength");
            return;
        }
        double durationSeconds = ElevatorUtil.parseSeconds(durationField.getValue());
        double nextSeconds = ElevatorUtil.parseSeconds(nextDelayField.getValue());
        double arrivalSeconds = ElevatorUtil.parseSeconds(arrivalDelayField.getValue());
        double maxSeconds = ElevatorUtil.MAX_TIME_TICKS / 20.0D;
        if (durationSeconds < 0.0D || durationSeconds > maxSeconds
                || nextSeconds < 0.0D || nextSeconds > maxSeconds
                || arrivalSeconds < 0.0D || arrivalSeconds > maxSeconds) {
            localError("message.cabin_elevator.interface.invalid_time", 0,
                    ElevatorUtil.MAX_TIME_TICKS / 20);
            return;
        }
        ElevatorNetwork.saveInterface(new SaveInterfacePacket(pos, newPriority, facesMask, newStrength,
                ElevatorUtil.secondsToTicks(durationSeconds), ElevatorUtil.secondsToTicks(nextSeconds),
                ElevatorUtil.secondsToTicks(arrivalSeconds)));
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

        renderBinding(graphics);
        GuiUtil.text(graphics, font, Component.translatable("gui.cabin_elevator.interface.priority"),
                left + 12, top + 53);
        GuiUtil.text(graphics, font, Component.translatable("gui.cabin_elevator.interface.faces"),
                left + 12, top + 77);
        GuiUtil.text(graphics, font, Component.translatable("gui.cabin_elevator.interface.strength"),
                left + 12, top + 123);
        GuiUtil.text(graphics, font, Component.translatable("gui.cabin_elevator.interface.duration"),
                left + 12, top + 147);
        GuiUtil.muted(graphics, font, Component.translatable("gui.cabin_elevator.seconds_short"),
                left + 155, top + 147);
        GuiUtil.text(graphics, font, Component.translatable("gui.cabin_elevator.interface.next_delay"),
                left + 12, top + 171);
        GuiUtil.muted(graphics, font, Component.translatable("gui.cabin_elevator.seconds_short"),
                left + 155, top + 171);
        GuiUtil.text(graphics, font, Component.translatable("gui.cabin_elevator.interface.arrival_delay"),
                left + 12, top + 195);
        if (priority == 1) {
            GuiUtil.muted(graphics, font, Component.translatable("gui.cabin_elevator.seconds_short"),
                    left + 155, top + 195);
        } else {
            GuiUtil.muted(graphics, font, Component.translatable("gui.cabin_elevator.interface.only_priority_one"),
                    left + 104, top + 195);
        }

        GuiUtil.subPanel(graphics, left + 250, top + 40, 144, 176);
        GuiUtil.text(graphics, font, Component.translatable("gui.cabin_elevator.interface.bound_list"),
                left + 256, top + 45);
        if (interfaceList == null) {
            GuiUtil.muted(graphics, font, Component.translatable("gui.cabin_elevator.interface.none"),
                    left + 256, top + 60);
        }

        if (status != null && Util.getMillis() < statusUntil) {
            graphics.drawString(font, status, left + 12, top + 234, GuiUtil.TEXT, false);
        } else {
            GuiUtil.muted(graphics, font, Component.translatable("gui.cabin_elevator.interface.hint"),
                    left + 12, top + 234);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderBinding(GuiGraphics graphics) {
        if (!snapshot.bound) {
            GuiUtil.text(graphics, font, Component.translatable("gui.cabin_elevator.interface.unbound")
                    .withStyle(ChatFormatting.GOLD), left + 12, top + 23);
            return;
        }
        if (!snapshot.controllerValid) {
            GuiUtil.text(graphics, font, Component.translatable("gui.cabin_elevator.interface.controller_invalid")
                    .withStyle(ChatFormatting.RED), left + 12, top + 23);
            if (snapshot.controllerPos != null) {
                GuiUtil.muted(graphics, font, Component.literal("(" + snapshot.controllerPos.getX() + ", "
                        + snapshot.controllerPos.getY() + ", " + snapshot.controllerPos.getZ() + ")"),
                        left + 12, top + 35);
            }
            return;
        }
        Component channel = snapshot.channel.isBlank()
                ? Component.translatable("gui.cabin_elevator.not_set")
                : Component.literal(clip(snapshot.channel, 110));
        Component floor = snapshot.floorName.isBlank()
                ? Component.translatable("gui.cabin_elevator.unnamed_floor")
                : Component.literal(clip(snapshot.floorName, 110));
        GuiUtil.text(graphics, font, Component.translatable("gui.cabin_elevator.interface.bound_to", channel, floor),
                left + 12, top + 23);
        GuiUtil.muted(graphics, font, Component.literal("(" + snapshot.controllerPos.getX() + ", "
                + snapshot.controllerPos.getY() + ", " + snapshot.controllerPos.getZ() + ")"),
                left + 12, top + 35);
    }

    private String clip(String text, int pixelWidth) {
        return text == null ? "" : font.plainSubstrByWidth(text, pixelWidth);
    }

    @Override
    public void tick() {
        super.tick();
        if (priorityField != null) {
            priorityField.tick();
        }
        if (strengthField != null) {
            strengthField.tick();
        }
        if (durationField != null) {
            durationField.tick();
        }
        if (nextDelayField != null) {
            nextDelayField.tick();
        }
        if (arrivalDelayField != null) {
            arrivalDelayField.tick();
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
