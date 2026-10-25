package org.lytharalab.gfbs.doors.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lytharalab.cabinelevator.client.GuiUtil;
import org.lytharalab.cabinelevator.client.WarmButton;
import org.lytharalab.gfbs.doors.content.DoorRedstoneMode;
import org.lytharalab.gfbs.doors.network.DoorConfigSnapshot;
import org.lytharalab.gfbs.doors.network.DoorNetwork;

public class DoorConfigScreen extends Screen {
    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 194;

    private DoorConfigSnapshot snapshot;
    private int left;
    private int top;

    public DoorConfigScreen(DoorConfigSnapshot snapshot) {
        super(Component.translatable("gui.gfbs_doors.config.title"));
        this.snapshot = snapshot;
    }

    public static void open(DoorConfigSnapshot snapshot, boolean openIfMissing) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DoorConfigScreen screen && screen.matches(snapshot)) {
            screen.applySnapshot(snapshot);
            return;
        }
        if (openIfMissing) {
            minecraft.setScreen(new DoorConfigScreen(snapshot));
        }
    }

    private boolean matches(DoorConfigSnapshot other) {
        return snapshot.pos().equals(other.pos());
    }

    public void applySnapshot(DoorConfigSnapshot snapshot) {
        this.snapshot = snapshot;
        if (minecraft != null) {
            rebuildWidgets();
        }
    }

    @Override
    protected void init() {
        super.init();
        this.left = (width - PANEL_WIDTH) / 2;
        this.top = (height - PANEL_HEIGHT) / 2;

        DoorRedstoneMode[] modes = DoorRedstoneMode.values();
        for (int i = 0; i < modes.length; i++) {
            DoorRedstoneMode mode = modes[i];
            int bx = left + 12 + (i % 2) * 92;
            int by = top + 50 + (i / 2) * 22;
            WarmButton button = new WarmButton(bx, by, 84, 18, Component.translatable(mode.translationKey()),
                    pressed -> saveMode(mode), Tooltip.create(Component.translatable(mode.tooltipKey())));
            button.setAccent(mode.ordinal() == snapshot.mode());
            addRenderableWidget(button);
        }

        boolean manual = snapshot.manual();
        WarmButton manualButton = new WarmButton(left + 12, top + 132, 176, 18,
                Component.translatable(manual ? "gui.gfbs_doors.config.manual.allowed"
                        : "gui.gfbs_doors.config.manual.denied"),
                pressed -> DoorNetwork.save(snapshot.pos(), snapshot.mode(), !snapshot.manual()),
                Tooltip.create(Component.translatable("gui.gfbs_doors.config.manual.tooltip")));
        manualButton.setAccent(manual);
        manualButton.active = !DoorRedstoneMode.byOrdinal(snapshot.mode()).isGfbs();
        addRenderableWidget(manualButton);

        addRenderableWidget(new WarmButton(left + 12, top + 170, 108, 16,
                Component.translatable("gui.gfbs_doors.config.force_toggle"),
                pressed -> DoorNetwork.toggle(snapshot.pos()),
                Tooltip.create(Component.translatable("gui.gfbs_doors.config.force_toggle.tooltip"))));
        addRenderableWidget(new WarmButton(left + 128, top + 170, 60, 16,
                Component.translatable("gui.gfbs_doors.close"), button -> onClose(), null));
    }

    private void saveMode(DoorRedstoneMode mode) {
        if (mode.ordinal() == snapshot.mode()) {
            return;
        }
        DoorNetwork.save(snapshot.pos(), mode.ordinal(), snapshot.manual());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        GuiUtil.panel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.drawCenteredString(font, title, left + PANEL_WIDTH / 2, top + 8, GuiUtil.ACCENT);
        graphics.drawCenteredString(font, Component.translatable("block.gfbs_doors." + snapshot.doorId()),
                left + PANEL_WIDTH / 2, top + 22, GuiUtil.MUTED);
        GuiUtil.text(graphics, font, Component.translatable("gui.gfbs_doors.config.redstone_mode"),
                left + 12, top + 38);
        GuiUtil.text(graphics, font, Component.translatable("gui.gfbs_doors.config.manual"),
                left + 12, top + 120);
        GuiUtil.muted(graphics, font, Component.translatable("gui.gfbs_doors.config.hint"),
                left + 12, top + 156);
        super.render(graphics, mouseX, mouseY, partialTick);
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
