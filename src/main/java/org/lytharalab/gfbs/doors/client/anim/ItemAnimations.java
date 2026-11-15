package org.lytharalab.gfbs.doors.client.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * Which animation the held item is playing, the 1.20.1 counterpart of HBM's {@code HbmAnimations}.
 *
 * <p>NTM kept one animation per hotbar slot, stamped with the item it belongs to and a start time, and a renderer
 * asked "what does {@code SWING_ROT} look like right now for the item in my hand". Nothing was sent over the
 * network and no entity was tracked: animations only ever played for the local player's own held item, and the
 * check for that is the item identity below.</p>
 *
 * <p>That also means this is deliberately client-only - a dedicated server has no business loading it.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class ItemAnimations {
    private static final int HOTBAR_SLOTS = 9;
    private static final Animation[] HOTBAR = new Animation[HOTBAR_SLOTS];

    private record Animation(Item item, long startedAt, BusAnimation bus) {
    }

    private ItemAnimations() {
    }

    public static void play(int slot, Item item, @Nullable BusAnimation bus) {
        if (bus == null || slot < 0 || slot >= HOTBAR_SLOTS) {
            return;
        }
        HOTBAR[slot] = new Animation(item, Util.getMillis(), bus);
    }

    /** True while the given track is running for the held item; used both by renderers and by the swing trigger. */
    public static boolean isPlaying(String bus) {
        return transformation(bus) != null;
    }

    /**
     * The nine values of one track at the current moment, or null when nothing is playing.
     *
     * <p>An animation is dropped as soon as it runs out instead of lingering on its last frame: NTM's clips all end
     * on their resting pose anyway, and keeping stale state around only makes debugging harder.</p>
     */
    @Nullable
    public static float[] transformation(String bus) {
        Animation animation = relevant();
        if (animation == null) {
            return null;
        }
        BusAnimationSequence sequence = animation.bus().bus(bus);
        if (sequence == null) {
            return null;
        }
        return sequence.transformation((int) (Util.getMillis() - animation.startedAt()));
    }

    public static void applyTranslation(PoseStack pose, String bus) {
        float[] values = transformation(bus);
        if (values != null) {
            pose.translate(values[BusAnimationSequence.Dimension.TX.ordinal()],
                values[BusAnimationSequence.Dimension.TY.ordinal()],
                values[BusAnimationSequence.Dimension.TZ.ordinal()]);
        }
    }

    /** NTM's rotation order, and HBM's {@code rotMode} default: X, then Y, then Z. */
    public static void applyRotation(PoseStack pose, String bus) {
        float[] values = transformation(bus);
        if (values == null) {
            return;
        }
        pose.mulPose(Axis.XP.rotationDegrees(values[BusAnimationSequence.Dimension.RX.ordinal()]));
        pose.mulPose(Axis.YP.rotationDegrees(values[BusAnimationSequence.Dimension.RY.ordinal()]));
        pose.mulPose(Axis.ZP.rotationDegrees(values[BusAnimationSequence.Dimension.RZ.ordinal()]));
    }

    /** A single channel, for tracks that drive a part rather than the whole item. */
    public static float channel(String bus, BusAnimationSequence.Dimension dimension) {
        float[] values = transformation(bus);
        return values == null ? 0.0F : values[dimension.ordinal()];
    }

    @Nullable
    private static Animation relevant() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        int slot = player.getInventory().selected;
        if (slot < 0 || slot >= HOTBAR_SLOTS) {
            return null;
        }
        Animation animation = HOTBAR[slot];
        if (animation == null) {
            return null;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || held.getItem() != animation.item()) {
            return null;
        }
        if (Util.getMillis() - animation.startedAt() > animation.bus().duration()) {
            HOTBAR[slot] = null;
            return null;
        }
        return animation;
    }

    public static void clear() {
        for (int slot = 0; slot < HOTBAR_SLOTS; slot++) {
            HOTBAR[slot] = null;
        }
    }
}
