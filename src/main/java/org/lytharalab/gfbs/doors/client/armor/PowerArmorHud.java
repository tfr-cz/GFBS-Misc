package org.lytharalab.gfbs.doors.client.armor;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.lytharalab.gfbs.doors.content.armor.PowerArmorCharge;

/**
 * The charge rows above the hotbar: HBM's {@code onHUDRenderBar}, rebuilt on top of Forge's overlay stack.
 *
 * <p>NTM painted these in {@code RenderGameOverlayEvent.Post(ARMOR)} and reserved space by bumping
 * {@code GuiIngameForge.left_height}. Forge's {@link ForgeGui} keeps the same field, so this renders as its own
 * overlay - registered <em>after</em> everything else and anchored to {@code leftHeight} - which is both the
 * closest thing to HBM's behaviour and the reason nothing can paint over the rows: they are last.</p>
 *
 * <p>An earlier version registered a bar inside Classic Bar's own render loop instead. That drew the rows but
 * flickered, because the two renderers fight over the same rows within one frame. Drawing outside that loop and
 * last removes the race; Classic Bar stays the owner of the bars below, and this simply stacks on top of
 * whatever {@code leftHeight} ended at.</p>
 *
 * <p>No text, no blinking, no sound - HBM has none of those. One deliberate difference: NTM hard-codes the fill
 * to green ({@code 65280}), while this ramps green to red with the charge, which is the colour the durability
 * bar it replaced has always used.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class PowerArmorHud implements IGuiOverlay {
    public static final int SEGMENT_WIDTH = 81;
    public static final int ROW_STEP = 4;
    public static final int HOTBAR_LEFT_OFFSET = 91;
    private static final int INSET = 1;
    private static final int TRACK_HEIGHT = 3;
    private static final int TRACK_COLOR = 0xFF404040;
    /** The four rows need this much vertical room; reserved from {@code leftHeight} for anything after us. */
    private static final int RESERVED_HEIGHT = ROW_STEP * 4;

    private static final boolean DEBUG = Boolean.getBoolean("gfbs.debug.hud");
    /** Without the flag a short unconditional burst is logged, so a report needs no launcher setup. */
    private static final int FREE_LOG_LINES = 200;
    private static int freeLogLines;
    private static long renderCalls;
    private static long lastLogTick = Long.MIN_VALUE;
    private static long[] lastLoggedCharges = new long[4];

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Player player = gui.getMinecraft().player;
        if (player == null || gui.getMinecraft().options.hideGui) {
            return;
        }
        java.util.List<ItemStack> armor = player.getInventory().armor;
        if (!PowerArmorCharge.hasFullSet(armor)) {
            return;
        }

        // Drawn last, but Classic Bar's colours or another overlay's tint may still be in the shader colour.
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int left = screenWidth / 2 - HOTBAR_LEFT_OFFSET;
        int baseline = screenHeight - gui.leftHeight;
        for (int index = 0; index < armor.size(); index++) {
            ItemStack piece = armor.get(index);
            if (!PowerArmorCharge.isPowerArmor(piece)) {
                continue;
            }
            drawRow(graphics, left, baseline - ROW_STEP * (index + 1), chargeRatio(piece));
        }
        // GuiGraphics batches vertices; the shader colour is global and is read when the batch is uploaded. Other
        // HUD code animates that colour every frame, so a late upload would tint the fills with somebody else's
        // colour - flush now, while the colour is still white, or the rows visibly pulse.
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.flush();
        gui.leftHeight += RESERVED_HEIGHT;

        renderCalls++;
        if (DEBUG || freeLogLines < FREE_LOG_LINES) {
            logWhenItChanges(player, armor, screenHeight, gui.leftHeight, baseline);
        }
    }

    /** One track plus its 1px fill. */
    public static void drawRow(GuiGraphics graphics, int left, int top, float progress) {
        graphics.fill(left, top, left + SEGMENT_WIDTH, top + TRACK_HEIGHT, TRACK_COLOR);
        int fillEnd = fillEnd(left, progress);
        if (fillEnd > left + INSET) {
            graphics.fill(left + INSET, top + INSET, fillEnd, top + INSET + 1, 0xFF000000 | fillColor(progress));
        }
    }

    public static float chargeRatio(ItemStack piece) {
        return (float) PowerArmorCharge.get(piece) / (float) PowerArmorCharge.MAX_CHARGE;
    }

    /** The fill colour for a charge ratio: the same green-to-red ramp the durability bar uses. */
    static int fillColor(float progress) {
        return PowerArmorCharge.barColor(progress);
    }

    /** Where the 1px fill line stops: the track inset by one pixel on both sides, times the charge ratio. */
    static int fillEnd(int left, float progress) {
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        return left + INSET + Math.round((SEGMENT_WIDTH - INSET * 2) * clamped);
    }

    /**
     * Logs every change of any rendered value, with enough context to tell the candidate causes apart: whether
     * the overlay draws once per frame ({@code calls}), whether the four stacks are the same objects
     * ({@code ids=}), which items they are ({@code items=}), and what the first stack's tag actually holds
     * ({@code tag0=}). Values that jump between a few arithmetic sequences mean several different stacks are
     * being read in turn, not that one value is oscillating.
     */
    private static void logWhenItChanges(Player player, java.util.List<ItemStack> armor, int screenHeight,
                                         int leftHeight, int baseline) {
        long[] charges = new long[armor.size()];
        StringBuilder tags = new StringBuilder();
        StringBuilder ids = new StringBuilder();
        StringBuilder items = new StringBuilder();
        boolean changed = false;
        for (int i = 0; i < armor.size(); i++) {
            ItemStack piece = armor.get(i);
            boolean power = PowerArmorCharge.isPowerArmor(piece);
            charges[i] = power ? PowerArmorCharge.get(piece) : -1L;
            boolean tagged = piece.getTag() != null && piece.getTag().contains("charge");
            tags.append(tagged ? 'T' : (power ? 'm' : '-'));
            ids.append(Integer.toHexString(System.identityHashCode(piece))).append(' ');
            items.append(piece.isEmpty() ? "empty" : piece.getItem().toString()).append(' ');
            if (i < lastLoggedCharges.length && charges[i] != lastLoggedCharges[i]) {
                changed = true;
            }
        }
        if (!changed) {
            return;
        }
        lastLoggedCharges = charges;
        freeLogLines++;
        ItemStack first = armor.isEmpty() ? ItemStack.EMPTY : armor.get(0);
        org.slf4j.LoggerFactory.getLogger("gfbs_doors").info(
            "power hud: tick={} calls={} screenH={} leftHeight={} baseline={} tags={} "
                + "charges={} {} {} {} {} who={}/{} ids={} items={} tag0={}",
            player.level().getGameTime(), renderCalls, screenHeight, leftHeight, baseline, tags,
            charges.length > 0 ? charges[0] : -1L, charges.length > 1 ? charges[1] : -1L,
            charges.length > 2 ? charges[2] : -1L, charges.length > 3 ? charges[3] : -1L,
            player.getGameProfile().getName(), player.getId(), ids, items, first.getTag());
    }
}
