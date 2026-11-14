package org.lytharalab.gfbs.doors.client.armor.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import com.mojang.blaze3d.systems.RenderSystem;
import tfar.classicbar.EventHandler;
import tfar.classicbar.api.BarOverlay;
import tfar.classicbar.api.BarRegistry;
import tfar.classicbar.api.BarSettings;
import tfar.classicbar.api.BarSide;
import tfar.classicbar.api.Color;
import tfar.classicbar.api.colorprovider.SingleColorProvider;
import tfar.classicbar.impl.BarInfo;
import tfar.classicbar.impl.overlays.templates.BarOverlayImpl;
import org.lytharalab.gfbs.doors.client.armor.PowerArmorHud;
import org.lytharalab.gfbs.doors.content.armor.PowerArmorCharge;

import java.util.List;

/**
 * Classic Bar integration: the T-51b charge rows as a Classic Bar, so they land in that mod's layout.
 *
 * <p>Classic Bar cancels the vanilla {@code minecraft:armor_level} overlay and draws its own bars in its own
 * stacking order, which is why the plain {@code RenderGuiOverlayEvent.Post} hook in {@link PowerArmorHud}
 * would never fire in a pack that has it. Registering here instead lets Classic Bar hand us its running
 * offset ({@code gui.leftHeight}) and push everything below us up by the space the four rows take.</p>
 *
 * <p>Loaded only when {@link ClassicBarPresence#PRESENT}; this class extends a Classic Bar type, so touching
 * it without the mod installed would fail to link.</p>
 */
public final class ClassicBarPowerBars extends BarOverlayImpl {
    private static final String NAME = "gfbs_power_armor";
    /** The four worn pieces, one row each, matching the vanilla-layout HUD. */
    private static final int ROWS = 4;

    private static final BarInfo INFO = BarInfo.createSimpleVanilla(
        NAME,
        PowerArmorCharge::hasFullSet,
        ClassicBarPowerBars::lowestWornCharge,
        player -> (float) PowerArmorCharge.MAX_CHARGE);

    /** {@code -Dgfbs.debug.hud=true} logs the four charges and the layout offset once a second. */
    private static final boolean DEBUG = Boolean.getBoolean("gfbs.debug.hud");
    private static long lastLogTick = Long.MIN_VALUE;

    private static final Codec<ClassicBarPowerBars> CODEC = RecordCodecBuilder.create(
        instance -> codecStart(instance).apply(instance, ClassicBarPowerBars::new));

    public ClassicBarPowerBars(BarSettings settings) {
        super(INFO, settings);
    }

    /** Registers the bar; call once from the mod constructor, before Classic Bar caches its bar list. */
    public static void register() {
        BarRegistry.registerBar(new ClassicBarPowerBars(BarSettings.getBuilder()
            .setSide(BarSide.LEFT)
            .setShowText(false)
            .setShowIcon(false)
            .setColorProvider(new SingleColorProvider(Color.WHITE))
            .build()));
    }

    @Override
    public Codec<? extends BarOverlay> codec() {
        return CODEC;
    }

    @Override
    public void renderBar(ForgeGui gui, GuiGraphics graphics, Player player, int screenWidth, int screenHeight, int vOffset) {
        // The bars are drawn inside Classic Bar's pass, after other bars have called Color#color2Gl, so pin the
        // shader colour to white and make sure blending is on: a leftover tint would dim the fill on some frames.
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        List<ItemStack> armor = player.getInventory().armor;
        int left = screenWidth / 2 - PowerArmorHud.HOTBAR_LEFT_OFFSET;
        for (int index = 0; index < armor.size(); index++) {
            ItemStack piece = armor.get(index);
            if (!PowerArmorCharge.isPowerArmor(piece)) {
                continue;
            }
            int top = screenHeight - vOffset - PowerArmorHud.ROW_STEP * (index + 1);
            PowerArmorHud.drawRow(graphics, left, top, PowerArmorHud.chargeRatio(piece));
        }
        // Classic Bar adds 10 for the bar it just rendered; the other rows need the rest or they overlap.
        EventHandler.increment(gui, getSide(), PowerArmorHud.ROW_STEP * (ROWS - 1) + 2);

        if (DEBUG) {
            logOnceASecond(player, armor, vOffset);
        }
    }

    private static void logOnceASecond(Player player, List<ItemStack> armor, int vOffset) {
        long now = player.level().getGameTime();
        if (now == lastLogTick || (lastLogTick != Long.MIN_VALUE && now - lastLogTick < 20L)) {
            return;
        }
        lastLogTick = now;
        StringBuilder builder = new StringBuilder("power hud: vOffset=").append(vOffset).append(" charges=");
        for (ItemStack piece : armor) {
            builder.append(PowerArmorCharge.isPowerArmor(piece) ? PowerArmorCharge.get(piece) : -1L).append(' ');
        }
        org.slf4j.LoggerFactory.getLogger("gfbs_doors").info(builder.toString());
    }

    /** The numerator is the emptiest piece, so the bar reads as "how charged is the set". */
    private static float lowestWornCharge(Player player) {
        float lowest = (float) PowerArmorCharge.MAX_CHARGE;
        for (ItemStack piece : player.getInventory().armor) {
            if (PowerArmorCharge.isPowerArmor(piece)) {
                lowest = Math.min(lowest, (float) PowerArmorCharge.get(piece));
            }
        }
        return lowest;
    }
}
