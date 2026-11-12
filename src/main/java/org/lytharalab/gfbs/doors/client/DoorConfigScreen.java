package org.lytharalab.gfbs.doors.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.lytharalab.gfbs.doors.client.gui.Draw;
import org.lytharalab.gfbs.doors.client.gui.ModeButton;
import org.lytharalab.gfbs.doors.content.DoorRedstoneMode;
import org.lytharalab.gfbs.doors.content.block.DoorBlockEntity;
import org.lytharalab.gfbs.doors.network.DoorConfigSnapshot;
import org.lytharalab.gfbs.doors.network.DoorNetwork;

public class DoorConfigScreen extends Screen {
    private static final int PANEL_W = 240;
    private static final int PAD = 8;
    private static final int HEADER_H = 26;
    private static final int GROUP_GAP = 6;
    private static final int TITLE_H = 10;
    private static final int ROW_H = 21;
    private static final int BTN_H = 20;
    private static final int FOOTER_H = 33;
    private static final int MODE_COLS = 2;
    private static final int MODE_GAP = 6;
    private DoorConfigSnapshot snapshot;
    private int left;
    private int top;
    private int panelH;
    private int modeGroupY;
    private int gfbsGroupY;
    private int optionGroupY;
    private int footerY;
    private int heightValueX;
    private int heightValueW;
    private int heightValueY;
    private static final DoorRedstoneMode[] NORMAL_MODES = new DoorRedstoneMode[]{
        DoorRedstoneMode.NONE, DoorRedstoneMode.PULSE, DoorRedstoneMode.TRIGGER, DoorRedstoneMode.INVERTED
    };
    private static final DoorRedstoneMode[] FORCED_MODES = new DoorRedstoneMode[]{DoorRedstoneMode.GFBS_NORMALLY_CLOSED, DoorRedstoneMode.GFBS_NORMALLY_OPEN};

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
        return this.snapshot.pos().equals(other.pos());
    }

    public void applySnapshot(DoorConfigSnapshot snapshot) {
        this.snapshot = snapshot;
        if (this.minecraft != null) {
            this.rebuildWidgets();
        }
    }

    protected void init() {
        super.init();
        boolean blast = this.isBlastDoor();
        int innerW = 224;
        int modeRows = (NORMAL_MODES.length + 2 - 1) / 2;
        this.modeGroupY = 40;
        this.gfbsGroupY = this.modeGroupY + 10 + modeRows * 21 + 6;
        this.optionGroupY = this.gfbsGroupY + 10 + 21 + 6;
        int optionRows = blast ? 2 : 1;
        this.footerY = this.optionGroupY + 10 + optionRows * 21 + 6;
        this.panelH = this.footerY + 33 + 8;
        this.left = (this.width - 240) / 2;
        this.top = (this.height - this.panelH) / 2;
        int btnW = (innerW - 6) / 2;

        for (int i = 0; i < NORMAL_MODES.length; i++) {
            DoorRedstoneMode mode = NORMAL_MODES[i];
            int bx = this.left + 8 + i % 2 * (btnW + 6);
            int by = this.top + this.modeGroupY + 10 + i / 2 * 21;
            this.addRenderableWidget(this.modeButton(mode, bx, by, btnW));
        }

        int forcedW = (innerW - 6) / 2;

        for (int i = 0; i < FORCED_MODES.length; i++) {
            DoorRedstoneMode mode = FORCED_MODES[i];
            int bx = this.left + 8 + i * (forcedW + 6);
            int by = this.top + this.gfbsGroupY + 10;
            this.addRenderableWidget(this.modeButton(mode, bx, by, forcedW));
        }

        boolean manual = this.snapshot.manual();
        int manualY = this.top + this.optionGroupY + 10;
        ModeButton manualBtn = new ModeButton(
            this.left + 8,
            manualY,
            innerW,
            20,
            Component.translatable(manual ? "gui.gfbs_doors.config.manual.allowed" : "gui.gfbs_doors.config.manual.denied"),
            manual,
            () -> DoorNetwork.save(this.snapshot.pos(), this.snapshot.mode(), !this.snapshot.manual(), this.snapshot.extraHeight()),
            Tooltip.create(Component.translatable("gui.gfbs_doors.config.manual.tooltip"))
        );
        manualBtn.active = !DoorRedstoneMode.byOrdinal(this.snapshot.mode()).isGfbs();
        this.addRenderableWidget(manualBtn);
        if (blast) {
            int hY = manualY + 21;
            int stepW = 22;
            int valueW = 26;
            int plusX = this.left + 240 - 8 - stepW;
            int valueX = plusX - 4 - valueW;
            int minusX = valueX - 4 - stepW;
            ModeButton minus = new ModeButton(
                minusX,
                hY,
                stepW,
                20,
                Component.literal("-"),
                false,
                () -> this.changeHeight(-1),
                Tooltip.create(Component.translatable("gui.gfbs_doors.blast_height.tooltip"))
            );
            minus.active = this.snapshot.extraHeight() > 0;
            this.addRenderableWidget(minus);
            ModeButton plus = new ModeButton(
                plusX,
                hY,
                stepW,
                20,
                Component.literal("+"),
                false,
                () -> this.changeHeight(1),
                Tooltip.create(Component.translatable("gui.gfbs_doors.blast_height.tooltip"))
            );
            plus.active = this.snapshot.extraHeight() < 12;
            this.addRenderableWidget(plus);
            this.heightValueX = valueX;
            this.heightValueW = valueW;
            this.heightValueY = hY;
        }

        int actionY = this.top + this.footerY;
        int toggleW = innerW - 74 - 6;
        this.addRenderableWidget(
            new ModeButton(
                this.left + 8,
                actionY,
                toggleW,
                20,
                Component.translatable("gui.gfbs_doors.config.force_toggle"),
                false,
                () -> DoorNetwork.toggle(this.snapshot.pos()),
                Tooltip.create(Component.translatable("gui.gfbs_doors.config.force_toggle.tooltip"))
            )
        );
        this.addRenderableWidget(new ModeButton(this.left + 240 - 8 - 74, actionY, 74, 20, Component.translatable("gui.gfbs_doors.close"), false, this::onClose, null));
    }

    private ModeButton modeButton(DoorRedstoneMode mode, int x, int y, int w) {
        return new ModeButton(
            x,
            y,
            w,
            20,
            Component.translatable(mode.translationKey()),
            mode.ordinal() == this.snapshot.mode(),
            () -> this.saveMode(mode),
            Tooltip.create(Component.translatable(mode.tooltipKey()))
        );
    }

    private boolean isBlastDoor() {
        return "blast_door".equals(this.snapshot.doorId());
    }

    private void changeHeight(int delta) {
        int next = Math.max(0, Math.min(12, this.snapshot.extraHeight() + delta));
        if (next != this.snapshot.extraHeight()) {
            DoorNetwork.save(this.snapshot.pos(), this.snapshot.mode(), this.snapshot.manual(), next);
        }
    }

    private void saveMode(DoorRedstoneMode mode) {
        if (mode.ordinal() != this.snapshot.mode()) {
            DoorNetwork.save(this.snapshot.pos(), mode.ordinal(), this.snapshot.manual(), this.snapshot.extraHeight());
        }
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int x = this.left;
        int y = this.top;
        int innerX = x + 8;
        int innerW = 224;
        Draw.panel(g, x, y, 240, this.panelH, -15000287, -12696755);
        Draw.text(g, this.font, this.title, innerX, y + 8);
        Draw.dim(g, this.font, Component.translatable("block.gfbs_doors." + this.snapshot.doorId()), innerX, y + 8 + 12);
        boolean gfbs = DoorRedstoneMode.byOrdinal(this.snapshot.mode()).isGfbs();
        Component stateLabel = Component.translatable(gfbs ? "gui.gfbs_doors.state.locked" : "gui.gfbs_doors.state.controllable");
        int stateW = this.font.width(stateLabel) + 10;
        Draw.status(g, this.font, stateLabel, x + 240 - 8 - stateW + 3, y + 8 + 1, gfbs ? -2513855 : -9453709);
        Draw.rule(g, innerX, y + 8 + 23, innerW);
        Draw.sectionTitle(g, this.font, Component.translatable("gui.gfbs_doors.config.redstone_mode"), innerX, y + this.modeGroupY);
        Draw.sectionTitle(g, this.font, Component.translatable("gui.gfbs_doors.config.group.forced"), innerX, y + this.gfbsGroupY);
        Draw.sectionTitle(g, this.font, Component.translatable("gui.gfbs_doors.config.group.options"), innerX, y + this.optionGroupY);
        if (this.isBlastDoor()) {
            Component label = Component.translatable("gui.gfbs_doors.blast_height");
            Draw.dim(g, this.font, label, innerX, this.heightValueY + 6);
            Component value = Component.literal(String.valueOf(this.snapshot.extraHeight()));
            Draw.colored(g, this.font, value, this.heightValueX - 8 - this.font.width(value), this.heightValueY + 6, -1512723);
        }

        Draw.dim(g, this.font, Component.translatable("gui.gfbs_doors.config.hint"), innerX, y + this.footerY + 20 + 4);
        super.render(g, mouseX, mouseY, partialTick);
    }

    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    public void tick() {
        super.tick();
        if (this.minecraft != null && this.minecraft.level != null) {
            BlockEntity target = this.minecraft.level.getBlockEntity(this.snapshot.pos());
            if (!(target instanceof DoorBlockEntity)) {
                this.onClose();
            }
        }
    }

    public boolean isPauseScreen() {
        return false;
    }
}
