package org.lytharalab.gfbs.doors.client.anim;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.gfbs.doors.content.ModItems;
import org.lytharalab.gfbs.doors.content.ModSounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * The Crucible's clips and when to start them, ported from {@code ItemCrucible#getAnimation} and
 * {@code ItemCrucible#onArmorTick}/{@code #onEntitySwing}.
 *
 * <p>NTM started these from the server and pushed them to the client with a packet. They are purely visual, so this
 * port starts them where they are used, on the client, off the local player's own state: a swing is detected from
 * {@code LivingEntity#swinging} and the equip clip from a change of the held item.</p>
 *
 * <p>Guard plates: HBM folded them shut (90 degrees) the moment the sword was drawn, held them for 800 ms, then
 * snapped them open in 50 ms. That is the whole equip clip.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class CrucibleAnimations {
    public static final String SWING_ROT = "SWING_ROT";
    public static final String SWING_TRANS = "SWING_TRANS";
    public static final String GUARD_ROT = "GUARD_ROT";

    private static final Random RANDOM = new Random();
    /**
     * The first few triggers are logged at INFO so a "the animation never plays" report can be told apart from
     * "the clip is wrong" without asking for a debug flag to be set.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger("gfbs_doors/crucible");
    private static final int LOGGED_TRIGGERS = 3;
    private static int logged;
    private static Item lastHeld;
    private static boolean wasSwinging;
    private static int lastSwingTime;

    private CrucibleAnimations() {
    }

    /** Called once per client tick; does nothing unless the Crucible is the held item. */
    public static void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            lastHeld = null;
            wasSwinging = false;
            return;
        }
        ItemStack held = player.getMainHandItem();
        Item item = held.getItem();
        boolean ours = item == ModItems.CRUCIBLE.get();
        if (!ours) {
            lastHeld = item;
            return;
        }
        int slot = player.getInventory().selected;
        // A swing starts when `swinging` turns on, or when swingTime jumps back (a second swing before the first
        // finished, which is what attack speed 4.0 looks like).
        boolean started = player.swinging
            && (!wasSwinging || (player.swingTime >= 0 && player.swingTime < lastSwingTime));
        if (started) {
            if (logged < LOGGED_TRIGGERS) {
                logged++;
                LOGGER.info("Crucible swing animation started (slot {})", slot);
            }
            ItemAnimations.play(slot, item, swing());
            player.playNotifySound(ModSounds.CRUCIBLE_SWING.get(), SoundSource.PLAYERS, 1.0F,
                0.8F + RANDOM.nextFloat() * 0.2F);
        }
        wasSwinging = player.swinging;
        lastSwingTime = player.swinging ? player.swingTime : Integer.MAX_VALUE;
        if (item != lastHeld) {
            ItemAnimations.play(slot, item, equip());
        }
        lastHeld = item;
    }

    /** {@code ItemCrucible#getAnimation} SWING: a tumble left, then right, then back to rest. */
    private static BusAnimation swing() {
        int offset = RANDOM.nextInt(80) - 20;
        return new BusAnimation()
            .addBus(SWING_ROT, new BusAnimationSequence()
                .addRot(90 - offset, 90 - offset, 35, 75)
                .addRot(90 + offset, 90 - offset, -45, 150)
                .addRot(0, 0, 0, 500))
            .addBus(SWING_TRANS, new BusAnimationSequence()
                .addPos(-3, 0, 0, 75)
                .addPos(8, 0, 0, 150)
                .addPos(0, 0, 0, 500));
    }

    /** {@code ItemCrucible#getAnimation} EQUIP: guards snap shut, hold, then open. */
    private static BusAnimation equip() {
        return new BusAnimation()
            .addBus(GUARD_ROT, new BusAnimationSequence()
                .addRot(90, 0, 1, 0)
                .addRot(90, 0, 1, 800)
                .addRot(0, 0, 1, 50));
    }
}
